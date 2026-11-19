package com.MyAnimaLog.Veterinary.domain.schedule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un bloqueo de agenda: festivo, vacaciones, cirugía programada. A diferencia de los horarios, son
 * <strong>instantes</strong> ({@code TIMESTAMPTZ}), no horas locales.
 *
 * <p>{@code employeeId == null} bloquea <strong>toda la clínica</strong>; con un {@code employeeId}
 * bloquea solo a esa persona. Es configuración, no dato clínico: se borra de verdad (no hay baja
 * lógica como en {@code clinic_services}).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ScheduleBlock {
    private UUID id;
    private UUID veterinaryId;
    private UUID employeeId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String reason;

    public boolean isWholeClinic() {
        return employeeId == null;
    }
}
