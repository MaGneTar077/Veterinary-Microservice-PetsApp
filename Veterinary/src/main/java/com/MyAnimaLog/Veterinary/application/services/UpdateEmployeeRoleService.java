package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UpdateEmployeeRoleUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateEmployeeRoleService implements UpdateEmployeeRoleUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Override
    public UpdateEmployeeRoleResponse updateRole(UUID employeeId, UpdateEmployeeRoleRequest request) {
        if (request.getRole() == null) {
            throw new InvalidEmployeeRoleException();
        }

        VeterinaryEmployee employee = employeeRepositoryPort.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        VeterinaryEmployee updated = employee.toBuilder()
                .role(request.getRole())
                .build();

        VeterinaryEmployee saved = employeeRepositoryPort.save(updated);

        return UpdateEmployeeRoleResponse.builder()
                .id(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .userId(saved.getUserId())
                .role(saved.getRole())
                .build();
    }
}
