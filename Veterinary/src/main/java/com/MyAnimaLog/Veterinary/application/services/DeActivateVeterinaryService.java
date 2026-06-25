package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.DeActivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.DeActivateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
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
