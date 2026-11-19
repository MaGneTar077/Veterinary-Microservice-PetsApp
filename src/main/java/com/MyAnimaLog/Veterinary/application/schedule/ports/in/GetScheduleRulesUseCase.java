package com.MyAnimaLog.Veterinary.application.schedule.ports.in;

import com.MyAnimaLog.Veterinary.application.schedule.dto.GetScheduleRulesResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface GetScheduleRulesUseCase {

    GetScheduleRulesResponse get(UUID veterinaryId, OffsetDateTime from, OffsetDateTime to, UUID employeeId);
}
