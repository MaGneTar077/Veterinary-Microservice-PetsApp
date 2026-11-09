package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.PatientAccessRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.PatientAccessRepositoryPort.AccessRow;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba contra Postgres real el único {@code @Query} (join {@code veterinary} ×
 * {@code veterinary_employee} × {@code professional_profiles} × {@code veterinary_patients}) del
 * que depende el endpoint interno {@code access}. Un alias o un join mal escrito compila igual y un
 * mock nunca lo ejecutaría — la razón de ser de este archivo (ver CLAUDE.md, "Las consultas
 * escritas a mano se prueban contra Postgres").
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Fase4APatientAccessPersistenceIntegrationTest {

    @Autowired
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Autowired
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Autowired
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    @Autowired
    private VeterinaryPatientRepositoryPort patientRepositoryPort;
    @Autowired
    private PatientAccessRepositoryPort accessRepositoryPort;
    @Autowired
    private EntityManager entityManager;

    private UUID persistClinic(VeterinaryStatus status) {
        UUID id = TestClinics.persist(veterinaryRepositoryPort);
        if (status != VeterinaryStatus.ACTIVE) {
            veterinaryRepositoryPort.findById(id).ifPresent(v ->
                    veterinaryRepositoryPort.save(v.toBuilder().status(status).build()));
        }
        return id;
    }

    private void persistEmployee(UUID veterinaryId, UUID userId, EmployeeRole role, boolean active) {
        employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(role)
                .active(active)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void persistProfile(UUID userId, LicenseStatus status) {
        professionalProfileRepositoryPort.save(ProfessionalProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fullName("Ana Pérez")
                .documentType(IdDocumentType.CC)
                .documentNumber("1234567890")
                .licenseNumber("LIC-" + UUID.randomUUID())
                .profession(Profession.MV)
                .licenseStatus(status)
                .build());
    }

    private void persistPatient(UUID veterinaryId, UUID petId, ConsentStatus status) {
        patientRepositoryPort.save(VeterinaryPatient.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .petId(petId)
                .consentStatus(status)
                .grantedBy(UUID.randomUUID())
                .grantedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    void findAccess_shouldBeEmpty_whenClinicDoesNotExist() {
        entityManager.flush();

        Optional<AccessRow> result = accessRepositoryPort.findAccess(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findAccess_shouldReturnClinicWithNullsAndStatus_whenUserIsNotAnEmployee() {
        UUID veterinaryId = persistClinic(VeterinaryStatus.ACTIVE);
        entityManager.flush();

        AccessRow row = accessRepositoryPort.findAccess(
                veterinaryId, UUID.randomUUID(), UUID.randomUUID()).orElseThrow();

        assertThat(row.veterinaryStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(row.employeeId()).isNull();
        assertThat(row.role()).isNull();
        assertThat(row.employeeActive()).isNull();
        assertThat(row.licenseStatus()).isNull();
        assertThat(row.petConsentStatus()).isNull();
    }

    @Test
    void findAccess_shouldReportInactiveEmployee() {
        UUID veterinaryId = persistClinic(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        persistEmployee(veterinaryId, userId, EmployeeRole.VETERINARIAN, false);
        persistPatient(veterinaryId, petId, ConsentStatus.GRANTED);
        entityManager.flush();

        AccessRow row = accessRepositoryPort.findAccess(veterinaryId, petId, userId).orElseThrow();

        assertThat(row.employeeId()).isNotNull();
        assertThat(row.employeeActive()).isFalse();
        assertThat(row.role()).isEqualTo(EmployeeRole.VETERINARIAN);
    }

    @Test
    void findAccess_shouldReportSuspendedClinic() {
        UUID veterinaryId = persistClinic(VeterinaryStatus.SUSPENDED);
        UUID userId = UUID.randomUUID();
        persistEmployee(veterinaryId, userId, EmployeeRole.OWNER, true);
        entityManager.flush();

        AccessRow row = accessRepositoryPort.findAccess(veterinaryId, UUID.randomUUID(), userId).orElseThrow();

        assertThat(row.veterinaryStatus()).isEqualTo(VeterinaryStatus.SUSPENDED);
        assertThat(row.employeeActive()).isTrue();
    }

    @Test
    void findAccess_shouldReportPetNeverShared() {
        UUID veterinaryId = persistClinic(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        persistEmployee(veterinaryId, userId, EmployeeRole.VETERINARIAN, true);
        entityManager.flush();

        AccessRow row = accessRepositoryPort.findAccess(veterinaryId, UUID.randomUUID(), userId).orElseThrow();

        assertThat(row.petConsentStatus()).isNull();
    }

    @Test
    void findAccess_shouldReportRevokedConsent() {
        UUID veterinaryId = persistClinic(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        persistEmployee(veterinaryId, userId, EmployeeRole.VETERINARIAN, true);
        persistPatient(veterinaryId, petId, ConsentStatus.REVOKED);
        entityManager.flush();

        AccessRow row = accessRepositoryPort.findAccess(veterinaryId, petId, userId).orElseThrow();

        assertThat(row.petConsentStatus()).isEqualTo(ConsentStatus.REVOKED);
    }

    @Test
    void findAccess_shouldReportNoLicense_whenUserHasNoProfessionalProfile() {
        UUID veterinaryId = persistClinic(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        persistEmployee(veterinaryId, userId, EmployeeRole.VETERINARIAN, true);
        entityManager.flush();

        AccessRow row = accessRepositoryPort.findAccess(veterinaryId, UUID.randomUUID(), userId).orElseThrow();

        assertThat(row.licenseStatus()).isNull();
    }

    @Test
    void findAccess_shouldReportVerifiedLicense() {
        UUID veterinaryId = persistClinic(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        persistEmployee(veterinaryId, userId, EmployeeRole.VETERINARIAN, true);
        persistProfile(userId, LicenseStatus.VERIFIED);
        persistPatient(veterinaryId, petId, ConsentStatus.GRANTED);
        entityManager.flush();

        AccessRow row = accessRepositoryPort.findAccess(veterinaryId, petId, userId).orElseThrow();

        assertThat(row.licenseStatus()).isEqualTo(LicenseStatus.VERIFIED);
        assertThat(row.petConsentStatus()).isEqualTo(ConsentStatus.GRANTED);
    }

    @Test
    void findAccess_shouldReportEverythingGranted_fullHappyPath() {
        UUID veterinaryId = persistClinic(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        persistEmployee(veterinaryId, userId, EmployeeRole.OWNER, true);
        persistProfile(userId, LicenseStatus.VERIFIED);
        persistPatient(veterinaryId, petId, ConsentStatus.GRANTED);
        entityManager.flush();

        AccessRow row = accessRepositoryPort.findAccess(veterinaryId, petId, userId).orElseThrow();

        assertThat(row.veterinaryStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(row.role()).isEqualTo(EmployeeRole.OWNER);
        assertThat(row.employeeActive()).isTrue();
        assertThat(row.licenseStatus()).isEqualTo(LicenseStatus.VERIFIED);
        assertThat(row.petConsentStatus()).isEqualTo(ConsentStatus.GRANTED);
    }

    @Test
    void findAccess_shouldNotLeakAnotherClinicsPatientRow() {
        UUID firstClinic = persistClinic(VeterinaryStatus.ACTIVE);
        UUID secondClinic = persistClinic(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        persistEmployee(firstClinic, userId, EmployeeRole.VETERINARIAN, true);
        // La mascota está compartida con la OTRA clínica, no con la del empleado.
        persistPatient(secondClinic, petId, ConsentStatus.GRANTED);
        entityManager.flush();

        AccessRow row = accessRepositoryPort.findAccess(firstClinic, petId, userId).orElseThrow();

        assertThat(row.petConsentStatus()).isNull();
    }
}
