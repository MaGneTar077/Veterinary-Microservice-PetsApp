package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.staff.dto.InternalVeterinaryMemberResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.GetInternalVeterinaryMembersUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeWithLicense;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.security.RolePermissions;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Listado de miembros para Calendar (CALENDAR_REQUESTS_TO_VETERINARY.md, pedido 4): mostrar el
 * nombre del empleado en la agenda y asignar empleados elegibles. Por defecto solo activos; con
 * {@code includeInactive=true} también los retirados (con {@code active=false}).
 *
 * <p>{@code licensed} sale de {@code professional_profiles.license_status == VERIFIED} por
 * {@code user_id} — el mismo origen que usa el endpoint interno {@code access}, nunca el claim
 * {@code vet_licensed} del token — en la misma consulta que trae el rol y el estado de la fila
 * ({@link VeterinaryEmployeeRepositoryPort#findAllWithLicense}).
 *
 * <p>{@code permissions} se calcula con {@link RolePermissions#resolve}, igual que en todo el resto
 * del código (nunca se duplica esa lógica); un empleado <strong>retirado</strong> siempre tiene
 * {@code permissions} vacío, sin importar su rol — mismo criterio que
 * {@code GetPatientAccessService}, que solo resuelve permisos para un empleado activo.
 */
@Service
@RequiredArgsConstructor
public class GetInternalVeterinaryMembersService implements GetInternalVeterinaryMembersUseCase {

    private static final Duration NAME_RESOLUTION_BUDGET = Duration.ofMillis(1500);

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final UserServicePort userServicePort;

    @Override
    public List<InternalVeterinaryMemberResponse> list(UUID veterinaryId, boolean includeInactive) {
        Veterinary clinic = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        List<EmployeeWithLicense> members = employeeRepositoryPort.findAllWithLicense(veterinaryId).stream()
                .filter(employee -> includeInactive || employee.active())
                .toList();

        Map<UUID, UserServicePort.UserInfo> names = BestEffortUserNames.resolve(userServicePort,
                members.stream().map(EmployeeWithLicense::userId).toList(), NAME_RESOLUTION_BUDGET);

        return members.stream()
                .map(employee -> toResponse(employee, names.get(employee.userId()), clinic.getStatus()))
                .toList();
    }

    private InternalVeterinaryMemberResponse toResponse(EmployeeWithLicense employee,
                                                          UserServicePort.UserInfo user,
                                                          VeterinaryStatus clinicStatus) {
        Set<Permission> permissions = employee.active()
                ? RolePermissions.resolve(employee.role(), employee.licensed(), clinicStatus)
                : Set.of();

        return InternalVeterinaryMemberResponse.builder()
                .employeeId(employee.employeeId())
                .userId(employee.userId())
                .name(user != null ? user.name() : null)
                .role(employee.role())
                .active(employee.active())
                .licensed(employee.licensed())
                .permissions(permissions.stream().map(Permission::name).toList())
                .build();
    }
}
