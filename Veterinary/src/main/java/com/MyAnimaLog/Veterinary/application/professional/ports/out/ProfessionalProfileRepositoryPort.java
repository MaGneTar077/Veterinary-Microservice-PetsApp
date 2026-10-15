package com.MyAnimaLog.Veterinary.application.professional.ports.out;

import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;

import java.util.Optional;
import java.util.UUID;

public interface ProfessionalProfileRepositoryPort {
    Optional<ProfessionalProfile> findById(UUID id);
    Optional<ProfessionalProfile> findByUserId(UUID userId);
    boolean existsByLicenseNumber(String licenseNumber);
    ProfessionalProfile save(ProfessionalProfile profile);
}
