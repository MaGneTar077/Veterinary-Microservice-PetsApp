package com.MyAnimaLog.Veterinary.application.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** {@code employeeId == null} = bloqueo de toda la clínica. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleBlockResponse {
    private UUID id;
    private UUID employeeId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String reason;
}
