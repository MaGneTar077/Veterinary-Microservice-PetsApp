package com.MyAnimaLog.Veterinary.application.schedule.ports.in;

import com.MyAnimaLog.Veterinary.application.schedule.dto.EmployeeScheduleResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;

import java.util.List;
import java.util.UUID;

public interface ReplaceEmployeeScheduleUseCase {
    EmployeeScheduleResponse replace(UUID veterinaryId, UUID employeeId, List<ScheduleEntryRequest> entries);
}
