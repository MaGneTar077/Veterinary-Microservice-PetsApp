package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.CreateEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.dto.CreateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.CreateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateEmployeeService implements CreateEmployeeUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Override
    public CreateEmployeeResponse create(CreateEmployeeRequest request) {
        if (request.getRole() == null) {
            throw new InvalidEmployeeRoleException();
        }

        Veterinary veterinary = veterinaryRepositoryPort.findById(request.getVeterinaryId())
                .orElseThrow(VeterinaryNotFoundException::new);

        if (!veterinary.getActive()) {
            throw new VeterinaryNotActiveException();
        }

        if (employeeRepositoryPort.existsByVeterinaryIdAndUserId(
                request.getVeterinaryId(), request.getUserId())) {
            throw new EmployeeAlreadyExistsException();
        }

        VeterinaryEmployee employee = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(request.getVeterinaryId())
                .userId(request.getUserId())
                .role(request.getRole())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        VeterinaryEmployee saved = employeeRepositoryPort.save(employee);

        return CreateEmployeeResponse.builder()
                .id(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .userId(saved.getUserId())
                .role(saved.getRole())
                .active(saved.getActive())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
