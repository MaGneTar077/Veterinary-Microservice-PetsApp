package com.MyAnimaLog.Veterinary.infrastructure.catalog.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.catalog.entity.ClinicServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicServiceJpaRepository extends JpaRepository<ClinicServiceEntity, UUID> {

    Optional<ClinicServiceEntity> findByVeterinaryIdAndId(UUID veterinaryId, UUID id);

    List<ClinicServiceEntity> findByVeterinaryId(UUID veterinaryId);

    List<ClinicServiceEntity> findByVeterinaryIdAndActiveTrue(UUID veterinaryId);

    /** Respeta el índice único parcial {@code ux_clinic_services_vet_name_active} (case-insensitive). */
    boolean existsByVeterinaryIdAndActiveTrueAndNameIgnoreCase(UUID veterinaryId, String name);
}
