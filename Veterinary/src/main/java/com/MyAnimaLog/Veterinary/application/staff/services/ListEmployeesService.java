package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.ListEmployeesResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ListEmployeesUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ver el equipo lo puede hacer cualquier miembro activo de la clínica, sin un permiso específico:
 * {@code RolePermissions} no tiene un permiso de lectura de personal, y el más cercano
 * ({@code REPORTS_VIEW}) solo lo tienen OWNER y ADMIN, lo que dejaría a un veterinario sin poder ver
 * con quién trabaja. Por eso usa {@code requireActiveMember} y no {@code require(..., permiso)}.
 */
@Service
@RequiredArgsConstructor
public class ListEmployeesService implements ListEmployeesUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final UserServicePort userServicePort;

    @Override
    public ListEmployeesResponse list(UUID veterinaryId, boolean includeInactive) {
        ClinicContext caller = authorizationService.requireActiveMember(veterinaryId);

        List<VeterinaryEmployee> employees = employeeRepositoryPort.findByVeterinaryId(veterinaryId).stream()
                .filter(employee -> includeInactive || Boolean.TRUE.equals(employee.getActive()))
                .sorted(Comparator.comparing(VeterinaryEmployee::getRole)
                        .thenComparing(VeterinaryEmployee::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // Una resolución por lote para toda la página en vez de una llamada por fila. Hoy el
        // adaptador itera por id porque el User service todavía no expone un endpoint en lote
        // (ver "Pendientes del lado del User service" en CLAUDE.md).
        Map<UUID, UserServicePort.UserInfo> users = userServicePort.getUsers(
                employees.stream().map(VeterinaryEmployee::getUserId).distinct().toList());

        return ListEmployeesResponse.builder()
                .employees(employees.stream()
                        .map(employee -> toDto(employee, users.get(employee.getUserId()), caller))
                        .toList())
                .build();
    }

    private ListEmployeesResponse.Employee toDto(VeterinaryEmployee employee,
                                                  UserServicePort.UserInfo user,
                                                  ClinicContext caller) {
        return ListEmployeesResponse.Employee.builder()
                .employeeId(employee.getId())
                .userId(employee.getUserId())
                .name(user != null ? user.name() : null)
                .email(user != null ? user.email() : null)
                .role(employee.getRole())
                .active(employee.getActive())
                .createdAt(employee.getCreatedAt())
                .removedAt(employee.getRemovedAt())
                .self(employee.getId().equals(caller.employeeId()))
                .build();
    }
}
