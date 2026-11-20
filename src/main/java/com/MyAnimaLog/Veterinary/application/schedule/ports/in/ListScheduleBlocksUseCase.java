package com.MyAnimaLog.Veterinary.application.schedule.ports.in;

import com.MyAnimaLog.Veterinary.application.schedule.dto.ListScheduleBlocksResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ListScheduleBlocksUseCase {
    ListScheduleBlocksResponse list(UUID veterinaryId, OffsetDateTime from, OffsetDateTime to);
}
