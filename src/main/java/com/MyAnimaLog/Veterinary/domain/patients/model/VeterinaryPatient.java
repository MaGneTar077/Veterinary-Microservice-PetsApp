package com.MyAnimaLog.Veterinary.domain.patients.model;

import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.PatientOrigin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consentimiento explícito del dueño para que UNA clínica vea UNA mascota. Es la fuente de verdad
 * del endpoint interno {@code access} que consultan Medical, Pets y Calendar: si no hay una fila
 * {@code GRANTED} para el par (clínica, mascota), ningún empleado de esa clínica tiene acceso, por
 * más permisos que le dé su rol.
 *
 * <p>Una sola fila por par (índice único {@code ux_patient_vet_pet}): revocar la marca
 * {@code REVOKED} + {@code revokedAt} y volver a compartir <strong>reactiva la misma fila</strong>
 * con un {@code grantedAt} nuevo.
 *
 * <p>{@code petName}/{@code species} son un snapshot de trazabilidad que se llena con la misma
 * llamada en lote a Pets que valida la propiedad. <strong>Nunca se sirven en lugar del dato vivo de
 * Pets</strong>: si Pets no responde, los endpoints de pacientes devuelven 503, no datos viejos.
 *
 * <p>{@code updatedAt} no se setea acá: la mantiene Hibernate con {@code @UpdateTimestamp} en
 * {@code VeterinaryPatientEntity}, así que al leer trae el valor de la base y al escribir se ignora.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class VeterinaryPatient {
    private UUID id;
    private UUID veterinaryId;
    private UUID petId;

    @Builder.Default
    private PatientOrigin origin = PatientOrigin.SHARED_BY_OWNER;

    @Builder.Default
    private ConsentStatus consentStatus = ConsentStatus.GRANTED;

    /** El dueño que compartió la mascota. {@code null} solo en el walk-in de la Fase 4B. */
    private UUID grantedBy;

    private LocalDateTime grantedAt;
    private LocalDateTime revokedAt;

    private String petName;
    private String species;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isGranted() {
        return consentStatus == ConsentStatus.GRANTED;
    }
}
