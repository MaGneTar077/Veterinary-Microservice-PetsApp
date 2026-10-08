package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.staff.dto.CreateEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.CreateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.CreateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateEmployeeService implements CreateEmployeeUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public CreateEmployeeResponse create(CreateEmployeeRequest request) {
        authorizationService.require(request.getVeterinaryId(), Permission.STAFF_MANAGE);

        if (request.getRole() == null) {
            throw new InvalidEmployeeRoleException();
        }
        if (request.getRole() == EmployeeRole.OWNER) {
            throw new InvalidEmployeeRoleException("Cannot assign the OWNER role through this endpoint");
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
