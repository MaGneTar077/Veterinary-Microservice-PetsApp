package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.RegisterVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterVeterinaryService implements RegisterVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    @Transactional
    public RegisterVeterinaryResponse registerVeterinary(RegisterVeterinaryRequest request) {
        AuthenticatedUser caller = authenticatedUserPort.current();

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

        // TODO(VET-10): the creator should be OWNER, not ADMIN — EmployeeRole doesn't have
        // OWNER yet, so ADMIN is the closest equivalent until that role exists.
        VeterinaryEmployee creatorAsAdmin = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(saved.getId())
                .userId(caller.userId())
                .role(EmployeeRole.ADMIN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        employeeRepositoryPort.save(creatorAsAdmin);

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
