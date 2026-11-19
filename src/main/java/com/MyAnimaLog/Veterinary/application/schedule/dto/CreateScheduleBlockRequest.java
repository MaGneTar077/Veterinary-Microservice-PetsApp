package com.MyAnimaLog.Veterinary.application.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** {@code employeeId == null} pide bloquear toda la clínica, y eso exige {@code CLINIC_CONFIGURE}. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateScheduleBlockRequest {
    private UUID employeeId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String reason;
}
