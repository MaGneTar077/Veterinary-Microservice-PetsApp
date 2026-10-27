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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactivateVeterinaryService implements ReactivateVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public ReactivateVeterinaryResponse reactivate(UUID veterinaryId) {
        authorizationService.requirePlatformAdmin();

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        VeterinaryStatus previousStatus = veterinary.getStatus();
        if (previousStatus != VeterinaryStatus.SUSPENDED) {
            throw new InvalidVeterinaryStatusTransitionException(
                    "Cannot reactivate a veterinary in status " + previousStatus);
        }

        Veterinary saved = veterinaryRepositoryPort.save(veterinary.toBuilder()
                .status(VeterinaryStatus.ACTIVE)
                .active(VeterinaryStatus.ACTIVE.impliesActiveFlag())
                .build());

        return ReactivateVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .status(saved.getStatus())
                .active(saved.getActive())
                .approvedAt(saved.getApprovedAt())
                .build();
    }
}
