package com.MyAnimaLog.Veterinary.application.schedule.ports.in;

import java.util.UUID;

public interface DeleteScheduleBlockUseCase {
    void delete(UUID veterinaryId, UUID blockId);
}
