package com.MyAnimaLog.Veterinary.application.schedule.ports.in;

import com.MyAnimaLog.Veterinary.application.schedule.dto.EmployeeScheduleResponse;

import java.util.UUID;

public interface GetEmployeeScheduleUseCase {
    EmployeeScheduleResponse get(UUID veterinaryId, UUID employeeId);
}
