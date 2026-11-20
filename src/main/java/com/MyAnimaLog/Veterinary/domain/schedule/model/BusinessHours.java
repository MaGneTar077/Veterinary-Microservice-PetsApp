package com.MyAnimaLog.Veterinary.domain.schedule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Una franja de atención de la clínica para un día de la semana. {@code dayOfWeek} es ISO:
 * 1 = lunes … 7 = domingo. Las horas son <strong>locales a la zona horaria de la clínica</strong>
 * ({@code veterinary.timezone}), no instantes: la columna es {@code TIME}, sin zona.
 *
 * <p>Puede haber varias franjas el mismo día (mañana y tarde). La semana se reemplaza completa, no
 * se edita franja por franja.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class BusinessHours {
    private UUID id;
    private UUID veterinaryId;
    private Integer dayOfWeek;
    private LocalTime opensAt;
    private LocalTime closesAt;
}
