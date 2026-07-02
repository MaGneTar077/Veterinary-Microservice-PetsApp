package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.DeActivateEmployeeResponse;

import java.util.UUID;

public interface DeActivateEmployeeUseCase {
    DeActivateEmployeeResponse deActivate(UUID employeeId);
}
