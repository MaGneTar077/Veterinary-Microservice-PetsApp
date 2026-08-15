package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.ActivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.ActivateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ActivateVeterinaryService implements ActivateVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Override
    public ActivateVeterinaryResponse activate(UUID veterinaryId) {
        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        Veterinary updated = veterinary.toBuilder()
                .active(true)
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(updated);

        return ActivateVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .active(saved.getActive())
                .build();
    }
}
