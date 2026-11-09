package com.MyAnimaLog.Veterinary.domain.patients.enums;

/**
 * Consentimiento del dueño para que una clínica vea una mascota
 * ({@code veterinary_patients.consent_status}).
 *
 * <p>Revocar es {@code REVOKED} + {@code revoked_at}, nunca un DELETE: el acceso de la clínica a la
 * historia clínica de esa mascota tiene que quedar auditable. Volver a compartir reactiva la misma
 * fila ({@code GRANTED} + nuevo {@code granted_at}), que es lo que permite el índice único pleno
 * sobre {@code (veterinary_id, pet_id)}.
 */
public enum ConsentStatus {
    GRANTED,
    REVOKED
}
