package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.UpdateEmployeeRoleUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateEmployeeRoleService implements UpdateEmployeeRoleUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public UpdateEmployeeRoleResponse updateRole(UUID veterinaryIdFromPath, UUID employeeId,
                                                  UpdateEmployeeRoleRequest request) {
        if (request.getRole() == null) {
            throw new InvalidEmployeeRoleException();
        }
        if (request.getRole() == EmployeeRole.OWNER) {
            throw new InvalidEmployeeRoleException("Cannot assign the OWNER role through this endpoint");
        }

        VeterinaryEmployee employee = employeeRepositoryPort.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);
        StaffManagementRules.requireSameClinic(employee, veterinaryIdFromPath, authorizationService);

        // El contexto trae rol y employeeId de la base, no del claim: un actor al que acaban de
        // bajar el rol o retirar no pasa de acá aunque su token siga vigente.
        ClinicContext actor = authorizationService.require(employee.getVeterinaryId(), Permission.STAFF_MANAGE);

        StaffManagementRules.requireCanManage(actor, employee);
        StaffManagementRules.requireCanAssignRole(actor, request.getRole());

        EmployeeRole previousRole = employee.getRole();
        VeterinaryEmployee saved = employeeRepositoryPort.save(employee.toBuilder()
                .role(request.getRole())
                .build());

        publishRoleUpdatedEvent(saved, previousRole);

        return UpdateEmployeeRoleResponse.builder()
                .id(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .userId(saved.getUserId())
                .role(saved.getRole())
                .build();
    }

    /** Le avisa al empleado, no a quien hizo el cambio. */
    private void publishRoleUpdatedEvent(VeterinaryEmployee employee, EmployeeRole previousRole) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("employeeId", employee.getId());
        payload.put("oldRole", previousRole.name());
        payload.put("newRole", employee.getRole().name());
        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.EMPLOYEE_ROLE_UPDATED,
                employee.getUserId(),
                null,
                payload,
                EventMetadata.of(employee.getVeterinaryId(), null)));
    }
}
