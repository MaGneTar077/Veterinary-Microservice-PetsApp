package com.MyAnimaLog.Veterinary.infrastructure.patients.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.clinic.entity.VeterinaryEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Query dedicado del endpoint interno {@code access} (CONTRATOS_COMPARTIDOS.md §3): una sola
 * consulta con {@code LEFT JOIN} a {@code veterinary_employee}, {@code professional_profiles} y
 * {@code veterinary_patients}, en vez de las 3-4 llamadas por puerto que usan otros servicios de
 * este estilo (p. ej. {@code GetVeterinaryMemberService}). Vale la pena la excepción: es,
 * literalmente, el endpoint más llamado del ecosistema (Medical lo consulta en cada operación
 * clínica) y el plan exige performance aquí.
 *
 * <p>{@code extends Repository} (no {@code JpaRepository}) a propósito: esta interfaz no gestiona
 * {@code VeterinaryEntity} de verdad —esa responsabilidad es de
 * {@code VeterinaryJpaRepository} en el módulo {@code clinic}— así que no se le agrega el CRUD
 * completo, solo este método.
 */
public interface PatientAccessJpaRepository extends Repository<VeterinaryEntity, UUID> {

    @Query("""
            SELECT v.status AS veterinaryStatus,
                   emp.id AS employeeId,
                   emp.role AS role,
                   emp.active AS employeeActive,
                   pp.licenseStatus AS licenseStatus,
                   patient.consentStatus AS petConsentStatus
            FROM VeterinaryEntity v
            LEFT JOIN VeterinaryEmployeeEntity emp
                   ON emp.veterinaryId = v.id AND emp.userId = :userId
            LEFT JOIN ProfessionalProfileEntity pp
                   ON pp.userId = :userId
            LEFT JOIN VeterinaryPatientEntity patient
                   ON patient.veterinaryId = v.id AND patient.petId = :petId
            WHERE v.id = :veterinaryId
            """)
    Optional<PatientAccessProjection> findAccess(@Param("veterinaryId") UUID veterinaryId,
                                                  @Param("petId") UUID petId,
                                                  @Param("userId") UUID userId);
}
