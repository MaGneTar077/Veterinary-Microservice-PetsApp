package com.MyAnimaLog.Veterinary.application.schedule.ports.in;

import com.MyAnimaLog.Veterinary.application.schedule.dto.CreateScheduleBlockRequest;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleBlockResponse;

import java.util.UUID;

public interface CreateScheduleBlockUseCase {
    ScheduleBlockResponse create(UUID veterinaryId, CreateScheduleBlockRequest request);
}
