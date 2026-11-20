package com.MyAnimaLog.Veterinary.infrastructure.staff.repositories;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.infrastructure.staff.entity.VeterinaryEmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeterinaryEmployeeJpaRepository extends JpaRepository<VeterinaryEmployeeEntity, UUID> {
    boolean existsByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    Optional<VeterinaryEmployeeEntity> findByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    List<VeterinaryEmployeeEntity> findByUserIdAndRole(UUID userId, EmployeeRole role);
    List<VeterinaryEmployeeEntity> findByUserIdAndActiveTrue(UUID userId);
    List<VeterinaryEmployeeEntity> findByVeterinaryId(UUID veterinaryId);
    List<VeterinaryEmployeeEntity> findByVeterinaryIdIn(List<UUID> veterinaryIds);
    Optional<VeterinaryEmployeeEntity> findByVeterinaryIdAndRoleAndActiveTrue(UUID veterinaryId, EmployeeRole role);
    List<VeterinaryEmployeeEntity> findByUserIdInAndActiveTrue(List<UUID> userIds);

    /**
     * Rol/estado del empleado y estado de la clínica en una sola consulta. Las dos entidades no
     * tienen una asociación JPA mapeada (el modelo usa ids, no referencias), así que el join va
     * explícito en el {@code WHERE}.
     */
    @Query("""
            SELECT e.id AS employeeId, e.role AS role, e.active AS active, v.status AS veterinaryStatus
            FROM VeterinaryEmployeeEntity e, VeterinaryEntity v
            WHERE v.id = e.veterinaryId
              AND e.veterinaryId = :veterinaryId
              AND e.userId = :userId
            """)
    Optional<ClinicMembershipProjection> findClinicMembership(@Param("veterinaryId") UUID veterinaryId,
                                                               @Param("userId") UUID userId);

    /**
     * {@code UPDATE} condicional del rol. {@code clearAutomatically} limpia el contexto de
     * persistencia para que una entidad ya cargada no quede con el rol viejo en la misma
     * transacción (igual que {@code updateStatusIfCurrentlyIs} en el módulo clinic).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VeterinaryEmployeeEntity e SET e.role = :newRole "
            + "WHERE e.id = :id AND e.role = :expectedRole AND e.active = true")
    int updateRoleIfCurrentlyIs(@Param("id") UUID id,
                                 @Param("expectedRole") EmployeeRole expectedRole,
                                 @Param("newRole") EmployeeRole newRole);

    /**
     * Mismo dato de licencia que usa {@code PatientAccessJpaRepository.findAccess}
     * ({@code professional_profiles.license_status} por {@code user_id}), para los empleados
     * activos de una clínica completa en una sola consulta.
     */
    @Query("""
            SELECT e.id AS employeeId, e.role AS role,
                   CASE WHEN pp.licenseStatus = com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus.VERIFIED
                        THEN true ELSE false END AS licensed
            FROM VeterinaryEmployeeEntity e
            LEFT JOIN ProfessionalProfileEntity pp ON pp.userId = e.userId
            WHERE e.veterinaryId = :veterinaryId AND e.active = true
            """)
    List<EmployeeLicenseProjection> findActiveWithLicense(@Param("veterinaryId") UUID veterinaryId);

    /**
     * Igual que {@link #findActiveWithLicense}, pero sin filtrar por {@code active} — el caller
     * (listado interno de miembros) decide en memoria si incluye a los retirados, para no pagar dos
     * consultas distintas.
     */
    @Query("""
            SELECT e.id AS employeeId, e.userId AS userId, e.role AS role, e.active AS active,
                   CASE WHEN pp.licenseStatus = com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus.VERIFIED
                        THEN true ELSE false END AS licensed
            FROM VeterinaryEmployeeEntity e
            LEFT JOIN ProfessionalProfileEntity pp ON pp.userId = e.userId
            WHERE e.veterinaryId = :veterinaryId
            ORDER BY e.role, e.createdAt
            """)
    List<EmployeeWithLicenseProjection> findAllWithLicense(@Param("veterinaryId") UUID veterinaryId);
}
