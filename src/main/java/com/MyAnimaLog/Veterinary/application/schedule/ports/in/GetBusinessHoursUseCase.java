package com.MyAnimaLog.Veterinary.application.schedule.ports.in;

import com.MyAnimaLog.Veterinary.application.schedule.dto.BusinessHoursResponse;

import java.util.UUID;

public interface GetBusinessHoursUseCase {
    BusinessHoursResponse get(UUID veterinaryId);
}
