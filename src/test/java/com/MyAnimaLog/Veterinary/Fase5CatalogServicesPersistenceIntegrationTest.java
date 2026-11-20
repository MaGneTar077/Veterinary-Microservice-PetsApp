package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra Postgres real: el índice único parcial {@code ux_clinic_services_vet_name_active} (009) y
 * la consulta {@code findActiveWithLicense} (join a {@code professional_profiles} por
 * {@code user_id}), que un mock nunca ejecutaría.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Fase5CatalogServicesPersistenceIntegrationTest {

    @Autowired
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Autowired
    private ClinicServiceRepositoryPort clinicServiceRepositoryPort;
    @Autowired
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Autowired
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    private ClinicService newService(UUID veterinaryId, String name, boolean active) {
        return ClinicService.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .name(name)
                .category(ServiceCategory.CONSULTATION)
                .durationMinutes(30)
                .price(BigDecimal.valueOf(40000))
                .bookableOnline(true)
                .active(active)
                .requiresLicense(false)
                .allowedRoles(List.of(EmployeeRole.OWNER, EmployeeRole.ADMIN, EmployeeRole.VETERINARIAN))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createAndList_shouldPersistAndReadBackTheService() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);

        ClinicService saved = clinicServiceRepositoryPort.save(newService(veterinaryId, "Consulta general", true));

        List<ClinicService> services = clinicServiceRepositoryPort.findByVeterinaryId(veterinaryId);

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getId()).isEqualTo(saved.getId());
        assertThat(services.get(0).getName()).isEqualTo("Consulta general");
        assertThat(services.get(0).getUpdatedAt()).isNotNull();
    }

    @Test
    void existsActiveByVeterinaryIdAndName_shouldBeTrue_whenAnotherActiveServiceHasTheSameName() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);
        clinicServiceRepositoryPort.save(newService(veterinaryId, "Vacunación", true));

        boolean exists = clinicServiceRepositoryPort.existsActiveByVeterinaryIdAndName(veterinaryId, "Vacunación");

        assertThat(exists).isTrue();
    }

    @Test
    void existsActiveByVeterinaryIdAndName_shouldBeFalse_whenTheOnlyMatchIsDeactivated() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);
        ClinicService deactivated = clinicServiceRepositoryPort.save(newService(veterinaryId, "Vacunación vieja", true));
        clinicServiceRepositoryPort.save(deactivated.toBuilder().active(false).build());

        boolean exists = clinicServiceRepositoryPort.existsActiveByVeterinaryIdAndName(veterinaryId, "Vacunación vieja");

        assertThat(exists).isFalse();
    }

    @Test
    void findActiveWithLicense_shouldExcludeRetiredEmployees_andReportLicenseFromProfessionalProfile() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);

        UUID licensedVetUserId = UUID.randomUUID();
        UUID unlicensedVetUserId = UUID.randomUUID();

        UUID licensedEmployeeId = employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).userId(licensedVetUserId)
                .role(EmployeeRole.VETERINARIAN).active(true).createdAt(LocalDateTime.now()).build()).getId();
        UUID unlicensedEmployeeId = employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).userId(unlicensedVetUserId)
                .role(EmployeeRole.VETERINARIAN).active(true).createdAt(LocalDateTime.now()).build()).getId();
        // Retirado: no debe aparecer en el resultado.
        employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN).active(false).createdAt(LocalDateTime.now())
                .removedAt(LocalDateTime.now()).build());

        professionalProfileRepositoryPort.save(ProfessionalProfile.builder()
                .id(UUID.randomUUID()).userId(licensedVetUserId).fullName("Ana Pérez")
                .documentType(IdDocumentType.CC).documentNumber("1234567890")
                .licenseNumber("LIC-" + UUID.randomUUID()).profession(Profession.MV)
                .licenseStatus(LicenseStatus.VERIFIED).build());
        professionalProfileRepositoryPort.save(ProfessionalProfile.builder()
                .id(UUID.randomUUID()).userId(unlicensedVetUserId).fullName("Luis Gómez")
                .documentType(IdDocumentType.CC).documentNumber("9876543210")
                .licenseNumber("LIC-" + UUID.randomUUID()).profession(Profession.MV)
                .licenseStatus(LicenseStatus.PENDING).build());

        List<EmployeeLicense> result = employeeRepositoryPort.findActiveWithLicense(veterinaryId);

        assertThat(result).hasSize(2);
        assertThat(result).filteredOn(e -> e.employeeId().equals(licensedEmployeeId))
                .extracting(EmployeeLicense::licensed).containsExactly(true);
        assertThat(result).filteredOn(e -> e.employeeId().equals(unlicensedEmployeeId))
                .extracting(EmployeeLicense::licensed).containsExactly(false);
    }
}
