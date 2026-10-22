package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
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
class GetMyProfessionalProfileServiceTest {

    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private GetMyProfessionalProfileService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "vet@example.com", true, false, Optional.empty()));
    }

    @Test
    void getMyProfile_shouldReturnProfile_whenItExists() {
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fullName("Juan Pérez")
                .documentType(IdDocumentType.CC)
                .documentNumber("123456789")
                .profession(Profession.MV)
                .licenseNumber("LIC-001")
                .licenseStatus(LicenseStatus.NOT_SUBMITTED)
                .build();
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(profile));

        ProfessionalProfileResponse response = service.getMyProfile();

        assertThat(response.getFullName()).isEqualTo("Juan Pérez");
        assertThat(response.getLicenseNumber()).isEqualTo("LIC-001");
    }

    @Test
    void getMyProfile_shouldThrowNotFound_whenNoneExists() {
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile())
                .isInstanceOf(ProfessionalProfileNotFoundException.class);
    }
}
