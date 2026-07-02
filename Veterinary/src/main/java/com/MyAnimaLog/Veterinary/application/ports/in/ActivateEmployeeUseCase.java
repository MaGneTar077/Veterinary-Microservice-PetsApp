package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.ActivateEmployeeResponse;

import java.util.UUID;

public interface ActivateEmployeeUseCase {
    ActivateEmployeeResponse activate(UUID employeeId);
}
