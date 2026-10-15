package com.MyAnimaLog.Veterinary.infrastructure.professional.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.professional.entity.ProfessionalProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfessionalProfileJpaRepository extends JpaRepository<ProfessionalProfileEntity, UUID> {
    Optional<ProfessionalProfileEntity> findByUserId(UUID userId);
    boolean existsByLicenseNumber(String licenseNumber);
}
