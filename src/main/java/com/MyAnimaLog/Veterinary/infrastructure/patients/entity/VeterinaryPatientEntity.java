package com.MyAnimaLog.Veterinary.infrastructure.patients.entity;

import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.PatientOrigin;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "veterinary_patients")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VeterinaryPatientEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "veterinary_id", nullable = false)
    private UUID veterinaryId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20)
    private PatientOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false, length = 20)
    private ConsentStatus consentStatus;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "pet_name", length = 120)
    private String petName;

    @Column(name = "species", length = 50)
    private String species;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * La mantiene Hibernate, no los servicios: la fila se <strong>reactiva</strong> al volver a
     * compartir una mascota revocada en vez de insertarse de nuevo, así que {@code created_at} no
     * alcanza para saber cuándo cambió por última vez. Con {@code @UpdateTimestamp} se actualiza en
     * cada INSERT y en cada UPDATE sin que ningún servicio tenga que acordarse — el valor que traiga
     * el mapper se ignora al escribir.
     *
     * <p>Corolario para quien agregue consultas nuevas: un {@code UPDATE} masivo en JPQL
     * <strong>no</strong> dispara esto (Hibernate no aplica las anotaciones de generación a las
     * sentencias masivas). Por eso la revocación en cascada guarda fila por fila — ver
     * {@code VeterinaryPatientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy}.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
