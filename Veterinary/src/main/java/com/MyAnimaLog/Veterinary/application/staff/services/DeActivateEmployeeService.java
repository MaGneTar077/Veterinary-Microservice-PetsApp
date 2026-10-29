package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.DeActivateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeActivateEmployeeService implements DeActivateEmployeeUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public DeActivateEmployeeResponse deActivate(UUID veterinaryIdFromPath, UUID employeeId) {
        VeterinaryEmployee employee = employeeRepositoryPort.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);
        StaffManagementRules.requireSameClinic(employee, veterinaryIdFromPath);

        ClinicContext actor = authorizationService.require(employee.getVeterinaryId(), Permission.STAFF_MANAGE);
        StaffManagementRules.requireCanManage(actor, employee);

        VeterinaryEmployee saved = employeeRepositoryPort.save(employee.toBuilder()
                .active(false)
                .removedAt(LocalDateTime.now())
                .build());

        publishDeactivatedEvent(saved);

        return DeActivateEmployeeResponse.builder()
                .id(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .userId(saved.getUserId())
                .role(saved.getRole())
                .active(saved.getActive())
                .build();
    }

    /** Le avisa al empleado retirado. */
    private void publishDeactivatedEvent(VeterinaryEmployee employee) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("employeeId", employee.getId());
        payload.put("role", employee.getRole().name());
        payload.put("removedAt", employee.getRemovedAt());
        payload.put("leftVoluntarily", false);
        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.EMPLOYEE_DEACTIVATED,
                employee.getUserId(),
                null,
                payload,
                EventMetadata.of(employee.getVeterinaryId(), null)));
    }
}
