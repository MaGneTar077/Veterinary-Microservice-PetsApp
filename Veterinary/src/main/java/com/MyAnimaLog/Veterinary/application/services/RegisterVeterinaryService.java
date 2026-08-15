package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.RegisterVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterVeterinaryService implements RegisterVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Override
    public RegisterVeterinaryResponse registerVeterinary(RegisterVeterinaryRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new InvalidVeterinaryNameException();
        }
        if (request.getEmail() == null || request.getEmail().isBlank()
                || !request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new InvalidVeterinaryEmailException();
        }

        if (veterinaryRepositoryPort.existsByName(request.getName())) {
            throw new VeterinaryAlreadyExistsException();
        }
        if (veterinaryRepositoryPort.existsByEmail(request.getEmail())) {
            throw new VeterinaryEmailAlreadyExistsException();
        }

        Veterinary veterinary = Veterinary.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(veterinary);

        return RegisterVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .city(saved.getCity())
                .phone(saved.getPhone())
                .email(saved.getEmail())
                .tenantId(saved.getTenantId())
                .active(saved.getActive())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
