package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.DeActivateEmployeeResponse;

import java.util.UUID;

public interface DeActivateEmployeeUseCase {
    DeActivateEmployeeResponse deActivate(UUID employeeId);
}
