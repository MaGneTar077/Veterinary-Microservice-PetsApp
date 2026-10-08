package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.ReactivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.ReactivateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryStatusTransitionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactivateVeterinaryService implements ReactivateVeterinaryUseCase {

    // TODO(VET-17): PENDING_DOCUMENTS/UNDER_REVIEW are allowed here only as a temporary bypass
    // until the verification flow exists — remove once PLATFORM_ADMIN approval goes through
    // VerificationRequest instead of this endpoint.
    private static final Set<VeterinaryStatus> REACTIVATABLE_FROM = EnumSet.of(
            VeterinaryStatus.SUSPENDED, VeterinaryStatus.PENDING_DOCUMENTS, VeterinaryStatus.UNDER_REVIEW);

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public ReactivateVeterinaryResponse reactivate(UUID veterinaryId) {
        authorizationService.requirePlatformAdmin();

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        VeterinaryStatus previousStatus = veterinary.getStatus();
        if (!REACTIVATABLE_FROM.contains(previousStatus)) {
            throw new InvalidVeterinaryStatusTransitionException(
                    "Cannot reactivate a veterinary in status " + previousStatus);
        }

        Veterinary.VeterinaryBuilder updatedBuilder = veterinary.toBuilder()
                .status(VeterinaryStatus.ACTIVE)
                .active(VeterinaryStatus.ACTIVE.impliesActiveFlag());

        if (previousStatus != VeterinaryStatus.SUSPENDED) {
            updatedBuilder.approvedAt(LocalDateTime.now());
        }

        Veterinary saved = veterinaryRepositoryPort.save(updatedBuilder.build());

        return ReactivateVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .status(saved.getStatus())
                .active(saved.getActive())
                .approvedAt(saved.getApprovedAt())
                .build();
    }
}
