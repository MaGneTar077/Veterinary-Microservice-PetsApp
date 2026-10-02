package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.UpdateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
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