package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.staff.dto.ActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ActivateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
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
