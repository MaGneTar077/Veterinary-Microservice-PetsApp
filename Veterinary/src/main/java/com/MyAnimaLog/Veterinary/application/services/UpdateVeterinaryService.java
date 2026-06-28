package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UpdateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateVeterinaryService implements UpdateVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Override
    public UpdateVeterinaryResponse update(UUID veterinaryId, UpdateVeterinaryRequest request) {

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        if (request.getName() != null && request.getName().isBlank()) {
            throw new InvalidVeterinaryNameException();
        }
        if (request.getEmail() != null) {
            if (request.getEmail().isBlank()
                    || !request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new InvalidVeterinaryEmailException();
            }
            if (veterinaryRepositoryPort.existsByEmailAndIdNot(request.getEmail(), veterinaryId)) {
                throw new VeterinaryEmailAlreadyExistsException();
            }
        }

        Veterinary updated = veterinary.toBuilder()
                .name(request.getName() != null ? request.getName() : veterinary.getName())
                .city(request.getCity() != null ? request.getCity() : veterinary.getCity())
                .phone(request.getPhone() != null ? request.getPhone() : veterinary.getPhone())
                .email(request.getEmail() != null ? request.getEmail() : veterinary.getEmail())
                .updatedAt(LocalDateTime.now())
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(updated);

        return UpdateVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .city(saved.getCity())
                .phone(saved.getPhone())
                .email(saved.getEmail())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}