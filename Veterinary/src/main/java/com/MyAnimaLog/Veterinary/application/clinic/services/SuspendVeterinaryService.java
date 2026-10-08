package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.SuspendVeterinaryUseCase;
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
public class SuspendVeterinaryService implements SuspendVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public SuspendVeterinaryResponse suspend(UUID veterinaryId) {
        authorizationService.requirePlatformAdmin();

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        if (veterinary.getStatus() != VeterinaryStatus.ACTIVE) {
            throw new InvalidVeterinaryStatusTransitionException(
                    "Cannot suspend a veterinary in status " + veterinary.getStatus());
        }

        Veterinary updated = veterinary.toBuilder()
                .status(VeterinaryStatus.SUSPENDED)
                .active(VeterinaryStatus.SUSPENDED.impliesActiveFlag())
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(updated);

        return SuspendVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .status(saved.getStatus())
                .active(saved.getActive())
                .build();
    }
}
