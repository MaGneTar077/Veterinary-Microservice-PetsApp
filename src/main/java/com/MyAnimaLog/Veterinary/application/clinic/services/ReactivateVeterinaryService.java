package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.ReactivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.ReactivateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.CannotManageOwnClinicException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotSuspendedException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactivateVeterinaryService implements ReactivateVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public ReactivateVeterinaryResponse reactivate(UUID veterinaryId) {
        authorizationService.requirePlatformAdmin();

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        AuthenticatedUser caller = authenticatedUserPort.current();
        if (employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, caller.userId())) {
            throw new CannotManageOwnClinicException();
        }

        if (veterinary.getStatus() != VeterinaryStatus.SUSPENDED) {
            throw new ClinicNotSuspendedException();
        }

        boolean claimed = veterinaryRepositoryPort.claimStatusTransition(
                veterinaryId, VeterinaryStatus.SUSPENDED, VeterinaryStatus.ACTIVE);
        if (!claimed) {
            throw new ClinicNotSuspendedException();
        }

        Veterinary saved = veterinaryRepositoryPort.save(veterinary.toBuilder()
                .status(VeterinaryStatus.ACTIVE)
                .active(VeterinaryStatus.ACTIVE.impliesActiveFlag())
                .suspendedAt(null)
                .suspensionReason(null)
                .build());

        publishReactivatedEvent(saved);

        return ReactivateVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .status(saved.getStatus())
                .active(saved.getActive())
                .approvedAt(saved.getApprovedAt())
                .build();
    }

    private void publishReactivatedEvent(Veterinary veterinary) {
        employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinary.getId(), EmployeeRole.OWNER)
                .ifPresent(owner -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("veterinaryName", veterinary.getName());
                    eventPublisherPort.publish(DomainEvent.create(
                            VeterinaryEventType.VETERINARY_REACTIVATED,
                            owner.getUserId(),
                            null,
                            payload,
                            EventMetadata.of(veterinary.getId(), null)));
                });
    }
}
