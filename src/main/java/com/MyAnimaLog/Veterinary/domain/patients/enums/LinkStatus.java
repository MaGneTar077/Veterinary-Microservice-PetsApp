package com.MyAnimaLog.Veterinary.domain.patients.enums;

/**
 * Estado de la vinculación entre un dueño y una clínica ({@code user_veterinary_link.status}).
 *
 * <p>Hasta la Fase 4A esta columna era texto libre sin CHECK y desvincularse <strong>borraba la
 * fila</strong>. Desde {@code 008_patients.sql} el ciclo de vida es explícito: desvincularse es
 * {@code UNLINKED} + {@code unlinked_at}, para que una clínica pueda explicar por qué dejó de ver a
 * un cliente y para que volver a vincularse reactive la misma fila en vez de insertar otra.
 */
public enum LinkStatus {
    LINKED,
    UNLINKED
}
