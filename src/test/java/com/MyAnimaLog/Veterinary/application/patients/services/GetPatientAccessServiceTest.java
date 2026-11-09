package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetPatientAccessResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.PatientAccessRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.PatientAccessRepositoryPort.AccessRow;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Cubre las combinaciones de CONTRATOS_COMPARTIDOS.md §3: {@code access} nunca lanza, y
 * {@code hasAccess} depende de empleado activo + clínica ACTIVE + mascota GRANTED, mientras que
 * {@code permissions} depende solo de rol + licencia + estado (no del consentimiento puntual).
 */
@ExtendWith(MockitoExtension.class)
class GetPatientAccessServiceTest {

    @Mock
    private PatientAccessRepositoryPort accessRepositoryPort;

    @InjectMocks
    private GetPatientAccessService getPatientAccessService;

    private final UUID veterinaryId = UUID.randomUUID();
    private final UUID petId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    private void mockRow(AccessRow row) {
        when(accessRepositoryPort.findAccess(veterinaryId, petId, userId)).thenReturn(Optional.of(row));
    }

    @Test
    void getAccess_shouldReturnNoAccess_whenClinicDoesNotExist() {
        when(accessRepositoryPort.findAccess(veterinaryId, petId, userId)).thenReturn(Optional.empty());

        GetPatientAccessResponse response = getPatientAccessService.getAccess(veterinaryId, petId, userId);

        assertThat(response.isHasAccess()).isFalse();
        assertThat(response.getVeterinaryStatus()).isNull();
        assertThat(response.getEmployeeId()).isNull();
        assertThat(response.getPermissions()).isEmpty();
    }

    @Test
    void getAccess_shouldReturnNoAccess_whenUserIsNotAnEmployee() {
        mockRow(new AccessRow(VeterinaryStatus.ACTIVE, null, null, null, null, ConsentStatus.GRANTED));

        GetPatientAccessResponse response = getPatientAccessService.getAccess(veterinaryId, petId, userId);

        assertThat(response.isHasAccess()).isFalse();
        assertThat(response.getVeterinaryStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(response.getPermissions()).isEmpty();
    }

    @Test
    void getAccess_shouldReturnNoAccess_whenEmployeeIsInactive() {
        mockRow(new AccessRow(VeterinaryStatus.ACTIVE, employeeId, EmployeeRole.VETERINARIAN, false,
                LicenseStatus.VERIFIED, ConsentStatus.GRANTED));

        GetPatientAccessResponse response = getPatientAccessService.getAccess(veterinaryId, petId, userId);

        assertThat(response.isHasAccess()).isFalse();
        assertThat(response.getPermissions()).isEmpty();
    }

    @Test
    void getAccess_shouldReturnNoAccess_whenClinicIsNotActive() {
        mockRow(new AccessRow(VeterinaryStatus.SUSPENDED, employeeId, EmployeeRole.OWNER, true,
                null, ConsentStatus.GRANTED));

        GetPatientAccessResponse response = getPatientAccessService.getAccess(veterinaryId, petId, userId);

        assertThat(response.isHasAccess()).isFalse();
        // La clínica no ACTIVE recorta a {CLINIC_CONFIGURE, STAFF_MANAGE, SUBSCRIPTION_MANAGE}.
        assertThat(response.getPermissions()).contains(Permission.CLINIC_CONFIGURE)
                .doesNotContain(Permission.CLINICAL_READ_BASIC);
    }

    @Test
    void getAccess_shouldReturnNoAccess_whenPetWasNeverShared() {
        mockRow(new AccessRow(VeterinaryStatus.ACTIVE, employeeId, EmployeeRole.VETERINARIAN, true,
                LicenseStatus.VERIFIED, null));

        GetPatientAccessResponse response = getPatientAccessService.getAccess(veterinaryId, petId, userId);

        assertThat(response.isHasAccess()).isFalse();
        // Los permisos sí se calculan: no dependen del consentimiento de esta mascota puntual.
        assertThat(response.getPermissions()).contains(Permission.CLINICAL_WRITE);
    }

    @Test
    void getAccess_shouldReturnNoAccess_whenConsentWasRevoked() {
        mockRow(new AccessRow(VeterinaryStatus.ACTIVE, employeeId, EmployeeRole.VETERINARIAN, true,
                LicenseStatus.VERIFIED, ConsentStatus.REVOKED));

        assertThat(getPatientAccessService.getAccess(veterinaryId, petId, userId).isHasAccess()).isFalse();
    }

    @Test
    void getAccess_shouldGrantAccess_whenEverythingLinesUp() {
        mockRow(new AccessRow(VeterinaryStatus.ACTIVE, employeeId, EmployeeRole.VETERINARIAN, true,
                LicenseStatus.VERIFIED, ConsentStatus.GRANTED));

        GetPatientAccessResponse response = getPatientAccessService.getAccess(veterinaryId, petId, userId);

        assertThat(response.isHasAccess()).isTrue();
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
        assertThat(response.isLicensed()).isTrue();
        assertThat(response.getPermissions()).contains(Permission.CLINICAL_READ, Permission.CLINICAL_WRITE,
                Permission.NURSING_WRITE, Permission.DOCUMENT_UPLOAD);
    }

    @Test
    void getAccess_shouldNotGrantClinicalWrite_whenVeterinarianIsNotLicensed() {
        mockRow(new AccessRow(VeterinaryStatus.ACTIVE, employeeId, EmployeeRole.VETERINARIAN, true,
                LicenseStatus.PENDING, ConsentStatus.GRANTED));

        GetPatientAccessResponse response = getPatientAccessService.getAccess(veterinaryId, petId, userId);

        assertThat(response.isHasAccess()).isTrue();
        assertThat(response.isLicensed()).isFalse();
        assertThat(response.getPermissions()).doesNotContain(Permission.CLINICAL_WRITE)
                .contains(Permission.NURSING_WRITE);
    }

    @Test
    void getAccess_shouldGrantClinicalWrite_whenVeterinarianIsLicensed() {
        mockRow(new AccessRow(VeterinaryStatus.ACTIVE, employeeId, EmployeeRole.VETERINARIAN, true,
                LicenseStatus.VERIFIED, ConsentStatus.GRANTED));

        assertThat(getPatientAccessService.getAccess(veterinaryId, petId, userId).getPermissions())
                .contains(Permission.CLINICAL_WRITE);
    }

    @Test
    void getAccess_shouldGiveReceptionistNoClinicalPermissions() {
        mockRow(new AccessRow(VeterinaryStatus.ACTIVE, employeeId, EmployeeRole.RECEPTIONIST, true,
                null, ConsentStatus.GRANTED));

        GetPatientAccessResponse response = getPatientAccessService.getAccess(veterinaryId, petId, userId);

        assertThat(response.isHasAccess()).isTrue();
        assertThat(response.getPermissions())
                .doesNotContain(Permission.CLINICAL_READ, Permission.CLINICAL_WRITE, Permission.NURSING_WRITE)
                .contains(Permission.CLINICAL_READ_BASIC);
    }
}
