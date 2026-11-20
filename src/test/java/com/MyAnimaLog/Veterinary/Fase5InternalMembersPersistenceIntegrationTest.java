package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeWithLicense;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra Postgres real: {@code findAllWithLicense} (bloque e2, Fase 5) es un {@code @Query} nuevo
 * — join a {@code professional_profiles} por {@code user_id}, sin filtrar por {@code active} a
 * propósito (el caller decide en memoria) — que un mock nunca ejecutaría.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Fase5InternalMembersPersistenceIntegrationTest {

    @Autowired
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Autowired
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Autowired
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    private UUID persistEmployee(UUID veterinaryId, UUID userId, EmployeeRole role, boolean active) {
        return employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).userId(userId)
                .role(role).active(active).createdAt(LocalDateTime.now())
                .removedAt(active ? null : LocalDateTime.now())
                .build()).getId();
    }

    private void persistVerifiedProfile(UUID userId) {
        professionalProfileRepositoryPort.save(ProfessionalProfile.builder()
                .id(UUID.randomUUID()).userId(userId).fullName("Ana Pérez")
                .documentType(IdDocumentType.CC).documentNumber("1234567890")
                .licenseNumber("LIC-" + UUID.randomUUID()).profession(Profession.MV)
                .licenseStatus(LicenseStatus.VERIFIED).build());
    }

    @Test
    void findAllWithLicense_shouldIncludeActiveAndRemovedEmployees_withTheirRealLicenseStatus() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);

        UUID licensedUserId = UUID.randomUUID();
        UUID unlicensedUserId = UUID.randomUUID();
        UUID removedUserId = UUID.randomUUID();

        UUID licensedEmployeeId = persistEmployee(veterinaryId, licensedUserId, EmployeeRole.VETERINARIAN, true);
        UUID unlicensedEmployeeId = persistEmployee(veterinaryId, unlicensedUserId, EmployeeRole.ASSISTANT, true);
        UUID removedEmployeeId = persistEmployee(veterinaryId, removedUserId, EmployeeRole.RECEPTIONIST, false);

        persistVerifiedProfile(licensedUserId);

        List<EmployeeWithLicense> result = employeeRepositoryPort.findAllWithLicense(veterinaryId);

        assertThat(result).hasSize(3);
        assertThat(result).filteredOn(e -> e.employeeId().equals(licensedEmployeeId))
                .extracting(EmployeeWithLicense::licensed, EmployeeWithLicense::active)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(true, true));
        assertThat(result).filteredOn(e -> e.employeeId().equals(unlicensedEmployeeId))
                .extracting(EmployeeWithLicense::licensed, EmployeeWithLicense::active)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(false, true));
        assertThat(result).filteredOn(e -> e.employeeId().equals(removedEmployeeId))
                .extracting(EmployeeWithLicense::active)
                .containsExactly(false);
    }
}
