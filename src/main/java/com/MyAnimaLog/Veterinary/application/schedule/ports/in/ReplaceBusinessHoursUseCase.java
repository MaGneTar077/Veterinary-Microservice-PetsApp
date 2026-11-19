package com.MyAnimaLog.Veterinary.application.schedule.ports.in;

import com.MyAnimaLog.Veterinary.application.schedule.dto.BusinessHoursResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;

import java.util.List;
import java.util.UUID;

public interface ReplaceBusinessHoursUseCase {
    BusinessHoursResponse replace(UUID veterinaryId, List<ScheduleEntryRequest> entries);
}
