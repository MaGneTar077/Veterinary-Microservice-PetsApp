package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidScheduleRangeException;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Regla única del rango {@code [from, to)} que aceptan los endpoints de bloqueos y
 * {@code schedule-rules}: los dos obligatorios, {@code to > from} y como máximo 92 días (un
 * trimestre). El techo existe para que nadie pida "todos los bloqueos de la historia" y se traiga
 * la tabla completa.
 */
public final class ScheduleRanges {

    public static final int MAX_DAYS = 92;

    private ScheduleRanges() {
    }

    public static void requireValid(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) {
            throw new InvalidScheduleRangeException("from and to are required");
        }
        if (!to.isAfter(from)) {
            throw new InvalidScheduleRangeException("to must be after from");
        }
        if (Duration.between(from, to).toDays() > MAX_DAYS) {
            throw new InvalidScheduleRangeException("The range cannot be longer than " + MAX_DAYS + " days");
        }
    }
}
