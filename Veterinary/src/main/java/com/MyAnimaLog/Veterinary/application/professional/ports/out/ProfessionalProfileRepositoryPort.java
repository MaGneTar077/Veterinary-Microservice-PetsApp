package com.MyAnimaLog.Veterinary.application.professional.ports.out;

import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalProfileRepositoryPort {
    Optional<ProfessionalProfile> findById(UUID id);
    Optional<ProfessionalProfile> findByUserId(UUID userId);
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumberAndUserIdNot(String licenseNumber, UUID userId);
    ProfessionalProfile save(ProfessionalProfile profile);

    /**
     * Page of professional profiles for the admin panel (Fase 2B Parte 4B), filtered by verification
     * status and by name/license search (never by email — the profile doesn't store one), ordered by
     * {@code createdAt} desc.
     */
    AdminProfessionalPage findForAdminList(Boolean verified, String search, int page, int size);

    record AdminProfessionalPage(List<ProfessionalProfile> items, long total) {
    }
}
