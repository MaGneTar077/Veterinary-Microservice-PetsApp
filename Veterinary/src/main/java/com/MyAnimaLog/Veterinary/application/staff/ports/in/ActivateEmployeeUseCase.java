package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.ActivateEmployeeResponse;

import java.util.UUID;

public interface ActivateEmployeeUseCase {
    ActivateEmployeeResponse activate(UUID employeeId);
}
