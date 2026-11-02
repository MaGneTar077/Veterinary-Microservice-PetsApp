package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.LeaveVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.LeaveVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OwnerCannotLeaveException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Irse de una clínica por decisión propia. Es el mismo efecto que un retiro
 * ({@code active=false} + {@code removed_at}), pero sin pedir {@code STAFF_MANAGE} y sin las guardas
 * de "a quién puedo tocar": acá el objetivo es siempre uno mismo, y el {@code employeeId} sale del
 * contexto (de la base), nunca de la petición.
 *
 * <p>Usa {@code requireMember} y no {@code requireActiveMember} a propósito: irse de una clínica
 * suspendida tiene que poder hacerse. El OWNER no puede irse sin transferir primero la propiedad.
 */
@Service
@RequiredArgsConstructor
public class LeaveVeterinaryService implements LeaveVeterinaryUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public LeaveVeterinaryResponse leave(UUID veterinaryId) {
        ClinicContext caller = authorizationService.requireMember(veterinaryId);

        if (caller.role() == EmployeeRole.OWNER) {
            throw new OwnerCannotLeaveException();
        }

        VeterinaryEmployee employee = employeeRepositoryPort.findById(caller.employeeId())
                .orElseThrow(EmployeeNotFoundException::new);

        VeterinaryEmployee saved = employeeRepositoryPort.save(employee.toBuilder()
                .active(false)
                .removedAt(LocalDateTime.now())
                .build());

        publishLeftEvent(saved);

        return LeaveVeterinaryResponse.builder()
                .employeeId(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .removedAt(saved.getRemovedAt())
                .build();
    }

    /**
     * Reusa {@code EMPLOYEE_DEACTIVATED} (no hay un tipo de evento para "se fue") pero dirigido al
     * OWNER activo, que es a quien le sirve enterarse, con {@code leftVoluntarily=true} para
     * distinguirlo del retiro que hace la clínica.
     */
    private void publishLeftEvent(VeterinaryEmployee employee) {
        employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(employee.getVeterinaryId(), EmployeeRole.OWNER)
                .ifPresent(owner -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("employeeId", employee.getId());
                    payload.put("role", employee.getRole().name());
                    payload.put("removedAt", employee.getRemovedAt());
                    payload.put("leftVoluntarily", true);
                    eventPublisherPort.publish(DomainEvent.create(
                            VeterinaryEventType.EMPLOYEE_DEACTIVATED,
                            owner.getUserId(),
                            null,
                            payload,
                            EventMetadata.of(employee.getVeterinaryId(), null)));
                });
    }
}
