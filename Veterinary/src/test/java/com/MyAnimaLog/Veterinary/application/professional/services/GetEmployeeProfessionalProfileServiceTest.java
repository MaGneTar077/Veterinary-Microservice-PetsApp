package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.EmployeeProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEmployeeProfessionalProfileServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private GetEmployeeProfessionalProfileService service;

    private UUID veterinaryId;
    private UUID employeeId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void getProfile_shouldReturnOnlyNonSensitiveFields_whenProfileExists() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(
                VeterinaryEmployee.builder().id(employeeId).veterinaryId(veterinaryId).userId(userId)
                        .role(EmployeeRole.VETERINARIAN).active(true).build()));
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(
                ProfessionalProfile.builder()
                        .userId(userId)
                        .fullName("Juan Pérez")
                        .documentNumber("123456789")
                        .profession(Profession.MV)
                        .licenseNumber("LIC-001")
                        .specialty("Cirugía")
                        .university("U. Nacional")
                        .licenseStatus(LicenseStatus.VERIFIED)
                        .build()));

        EmployeeProfessionalProfileResponse response = service.getProfile(veterinaryId, employeeId);

        assertThat(response.isExists()).isTrue();
        assertThat(response.getFullName()).isEqualTo("Juan Pérez");
        assertThat(response.getLicenseNumber()).isEqualTo("LIC-001");
        assertThat(response.getLicenseStatus()).isEqualTo(LicenseStatus.VERIFIED);
    }

    @Test
    void getProfile_shouldReturnExistsFalse_whenEmployeeHasNoProfile() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(
                VeterinaryEmployee.builder().id(employeeId).veterinaryId(veterinaryId).userId(userId).build()));
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());

        EmployeeProfessionalProfileResponse response = service.getProfile(veterinaryId, employeeId);

        assertThat(response.isExists()).isFalse();
        assertThat(response.getFullName()).isNull();
    }

    @Test
    void getProfile_shouldThrowEmployeeNotFound_whenEmployeeBelongsToAnotherVeterinary() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(
                VeterinaryEmployee.builder().id(employeeId).veterinaryId(UUID.randomUUID()).userId(userId).build()));

        assertThatThrownBy(() -> service.getProfile(veterinaryId, employeeId))
                .isInstanceOf(EmployeeNotFoundException.class);
    }
}
