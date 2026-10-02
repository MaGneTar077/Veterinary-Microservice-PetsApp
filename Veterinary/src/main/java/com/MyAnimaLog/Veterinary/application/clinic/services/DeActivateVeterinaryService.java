package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeActivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.DeActivateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeActivateVeterinaryService implements DeActivateVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Override
    public DeActivateVeterinaryResponse deActivate(UUID veterinaryId) {
        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        Veterinary updated = veterinary.toBuilder()
                .active(false)
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(updated);

        return DeActivateVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .active(saved.getActive())
                .build();
    }
}
