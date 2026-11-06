package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Reproduces and verifies the fix for the same bug on {@code GET /api/veterinary/admin/professionals}:
 * {@code ProfessionalProfileJpaRepository.findForAdminList} used to be a JPQL query with
 * {@code (:verified IS NULL OR ...)}/{@code (:search IS NULL OR ...)} guards. See
 * {@code AdminClinicsListingFilterPersistenceIntegrationTest} for the full explanation; this class
 * exercises the same fix ({@code ProfessionalProfileAdminSpecifications}) against the real database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminProfessionalsListingFilterPersistenceIntegrationTest {

    @Autowired
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    private ProfessionalProfile persist(String marker, LicenseStatus licenseStatus) {
        return professionalProfileRepositoryPort.save(ProfessionalProfile.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .fullName("Veterinario " + marker)
                .documentType(IdDocumentType.CC)
                .documentNumber(String.valueOf(Math.abs(marker.hashCode())))
                .licenseNumber("LIC-" + marker)
                .profession(Profession.MVZ)
                .licenseStatus(licenseStatus)
                .build());
    }

    @Test
    void findForAdminList_shouldNotFail_whenNoFiltersAreProvided() {
        persist(unique("NOFILTER"), LicenseStatus.VERIFIED);

        assertThatCode(() -> professionalProfileRepositoryPort.findForAdminList(null, null, 0, 50))
                .doesNotThrowAnyException();
    }

    @Test
    void findForAdminList_shouldFilterByVerifiedTrueOnly() {
        String marker = unique("VERIFIED");
        ProfessionalProfile verified = persist(marker, LicenseStatus.VERIFIED);

        ProfessionalProfileRepositoryPort.AdminProfessionalPage page =
                professionalProfileRepositoryPort.findForAdminList(true, "LIC-" + marker, 0, 50);

        assertThat(page.items()).extracting(ProfessionalProfile::getId).containsExactly(verified.getId());
    }

    @Test
    void findForAdminList_shouldFilterByVerifiedFalseOnly() {
        String marker = unique("PENDING");
        ProfessionalProfile pending = persist(marker, LicenseStatus.PENDING);

        ProfessionalProfileRepositoryPort.AdminProfessionalPage page =
                professionalProfileRepositoryPort.findForAdminList(false, "LIC-" + marker, 0, 50);

        assertThat(page.items()).extracting(ProfessionalProfile::getId).containsExactly(pending.getId());
    }

    @Test
    void findForAdminList_shouldFilterBySearchOnly() {
        String marker = unique("SEARCH");
        ProfessionalProfile profile = persist(marker, LicenseStatus.NOT_SUBMITTED);

        ProfessionalProfileRepositoryPort.AdminProfessionalPage page =
                professionalProfileRepositoryPort.findForAdminList(null, "veterinario " + marker.toLowerCase(), 0, 50);

        assertThat(page.items()).extracting(ProfessionalProfile::getId).containsExactly(profile.getId());
    }

    @Test
    void findForAdminList_shouldFilterByVerifiedAndSearchCombined() {
        String marker = unique("COMBINED");
        ProfessionalProfile profile = persist(marker, LicenseStatus.VERIFIED);

        ProfessionalProfileRepositoryPort.AdminProfessionalPage page =
                professionalProfileRepositoryPort.findForAdminList(true, "lic-" + marker.toLowerCase(), 0, 50);

        assertThat(page.items()).extracting(ProfessionalProfile::getId).containsExactly(profile.getId());
    }

    @Test
    void findForAdminList_shouldTreatBlankSearchAsAbsent() {
        String marker = unique("BLANK");
        ProfessionalProfile profile = persist(marker, LicenseStatus.VERIFIED);

        assertThatCode(() -> professionalProfileRepositoryPort.findForAdminList(null, "   ", 0, 50))
                .doesNotThrowAnyException();

        ProfessionalProfileRepositoryPort.AdminProfessionalPage page =
                professionalProfileRepositoryPort.findForAdminList(true, "   ", 0, 50);
        assertThat(page.items()).extracting(ProfessionalProfile::getId).contains(profile.getId());
    }

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }
}
