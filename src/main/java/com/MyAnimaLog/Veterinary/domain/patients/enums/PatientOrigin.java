package com.MyAnimaLog.Veterinary.domain.patients.enums;

/**
 * Cómo llegó una mascota a ser paciente de una clínica ({@code veterinary_patients.origin}).
 *
 * <p>{@code REGISTERED_BY_CLINIC} existe desde {@code 008_patients.sql} pero <strong>nadie lo
 * escribe todavía</strong>: es para los pacientes <em>walk-in</em> de la Fase 4B (la clínica
 * registra la mascota de un dueño que no tiene la app, con {@code granted_by = null}). Está acá y en
 * el CHECK de la base desde ahora para que esa fase no necesite otra migración.
 */
public enum PatientOrigin {
    SHARED_BY_OWNER,
    REGISTERED_BY_CLINIC
}
