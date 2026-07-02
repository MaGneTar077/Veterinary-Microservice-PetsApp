package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.ActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.ActivateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivateEmployeeService implements ActivateEmployeeUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Override
    public ActivateEmployeeResponse activate(UUID employeeId) {

        VeterinaryEmployee employee = employeeRepositoryPort.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        VeterinaryEmployee updated = employee.toBuilder()
                .active(true)
                .build();

        VeterinaryEmployee saved = employeeRepositoryPort.save(updated);

        return ActivateEmployeeResponse.builder()
                .id(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .userId(saved.getUserId())
                .role(saved.getRole())
                .active(saved.getActive())
                .build();
    }
}
