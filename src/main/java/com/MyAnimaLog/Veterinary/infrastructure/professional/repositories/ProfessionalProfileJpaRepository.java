package com.MyAnimaLog.Veterinary.infrastructure.professional.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.professional.entity.ProfessionalProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProfessionalProfileJpaRepository extends JpaRepository<ProfessionalProfileEntity, UUID> {
    Optional<ProfessionalProfileEntity> findByUserId(UUID userId);
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumberAndUserIdNot(String licenseNumber, UUID userId);

    @Query("SELECT p FROM ProfessionalProfileEntity p WHERE "
            + "(:verified IS NULL OR (:verified = TRUE AND p.licenseStatus = 'VERIFIED') "
            + "OR (:verified = FALSE AND p.licenseStatus <> 'VERIFIED')) AND "
            + "(:search IS NULL OR LOWER(p.fullName) LIKE :search OR LOWER(p.licenseNumber) LIKE :search) "
            + "ORDER BY p.createdAt DESC")
    Page<ProfessionalProfileEntity> findForAdminList(@Param("verified") Boolean verified,
                                                       @Param("search") String search,
                                                       Pageable pageable);
}
