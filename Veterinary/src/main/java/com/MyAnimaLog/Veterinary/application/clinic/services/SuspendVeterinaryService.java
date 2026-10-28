package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.SuspendVeterinaryUseCase;
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
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidSuspensionReasonException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuspendVeterinaryService implements SuspendVeterinaryUseCase {

    private static final int MIN_REASON_LENGTH = 10;
    private static final int MAX_REASON_LENGTH = 500;

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final EventPublisherPort eventPublisherPort;

    @Value("${app.support-email}")
    private String supportEmail;

    @Override
    @Transactional
    public SuspendVeterinaryResponse suspend(UUID veterinaryId, SuspendVeterinaryRequest request) {
        authorizationService.requirePlatformAdmin();

        String reason = request.getReason();
        if (reason == null || reason.length() < MIN_REASON_LENGTH || reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidSuspensionReasonException(
                    "reason must be between " + MIN_REASON_LENGTH + " and " + MAX_REASON_LENGTH + " characters");
        }

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        AuthenticatedUser caller = authenticatedUserPort.current();
        if (employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, caller.userId())) {
            throw new CannotManageOwnClinicException();
        }

        if (veterinary.getStatus() != VeterinaryStatus.ACTIVE) {
            throw new ClinicNotActiveException();
        }

        boolean claimed = veterinaryRepositoryPort.claimStatusTransition(
                veterinaryId, VeterinaryStatus.ACTIVE, VeterinaryStatus.SUSPENDED);
        if (!claimed) {
            throw new ClinicNotActiveException();
        }

        LocalDateTime now = LocalDateTime.now();
        Veterinary saved = veterinaryRepositoryPort.save(veterinary.toBuilder()
                .status(VeterinaryStatus.SUSPENDED)
                .active(VeterinaryStatus.SUSPENDED.impliesActiveFlag())
                .suspendedAt(now)
                .suspensionReason(reason)
                .build());

        publishSuspendedEvent(saved, reason);

        return SuspendVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .status(saved.getStatus())
                .active(saved.getActive())
                .suspendedAt(saved.getSuspendedAt())
                .suspensionReason(saved.getSuspensionReason())
                .build();
    }

    private void publishSuspendedEvent(Veterinary veterinary, String reason) {
        employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinary.getId(), EmployeeRole.OWNER)
                .ifPresent(owner -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("veterinaryName", veterinary.getName());
                    payload.put("reason", reason);
                    payload.put("supportEmail", supportEmail);
                    eventPublisherPort.publish(DomainEvent.create(
                            VeterinaryEventType.VETERINARY_SUSPENDED,
                            owner.getUserId(),
                            null,
                            payload,
                            EventMetadata.of(veterinary.getId(), null)));
                });
    }
}
