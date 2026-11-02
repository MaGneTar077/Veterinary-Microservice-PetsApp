package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
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

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

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
    void getMyProfile_shouldMarkUnrequestedDocumentsReviewed_andExposeRejectionReasonCode_whenNeedsCorrection() {
        UUID profileId = UUID.randomUUID();
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .id(profileId).userId(userId).fullName("Juan Pérez")
                .documentType(IdDocumentType.CC).documentNumber("123456789")
                .profession(Profession.MV).licenseNumber("LIC-001")
                .licenseStatus(LicenseStatus.REJECTED)
                .build();
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(profile));

        LocalDateTime correctionRequestedAt = LocalDateTime.now().minusHours(1);
        VerificationRequest request = VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(SubjectType.PROFESSIONAL)
                .subjectId(profileId)
                .status(VerificationRequestStatus.NEEDS_CORRECTION)
                .attemptNumber(1)
                .correctionDocumentTypes(List.of("ID_DOCUMENT"))
                .correctionRequestedAt(correctionRequestedAt)
                .rejectionReasonCode(RejectionReasonCode.DOCUMENT_INVALID)
                .build();
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profileId))
                .thenReturn(Optional.of(request));

        VerificationDocument card = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(request.getId()).documentType("PROFESSIONAL_CARD")
                .storagePath("verifications/req/card.pdf").uploadedAt(correctionRequestedAt.minusDays(1)).build();
        VerificationDocument idDocument = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(request.getId()).documentType("ID_DOCUMENT")
                .storagePath("verifications/req/id.pdf").uploadedAt(correctionRequestedAt.minusDays(1)).build();
        when(verificationDocumentRepositoryPort.findByRequestId(request.getId())).thenReturn(List.of(card, idDocument));

        ProfessionalProfileResponse response = service.getMyProfile();

        assertThat(response.getRejectionReasonCode()).isEqualTo(RejectionReasonCode.DOCUMENT_INVALID);
        String cardStatus = response.getDocuments().stream()
                .filter(d -> d.getDocumentType().equals("PROFESSIONAL_CARD")).findFirst().orElseThrow().getStatus();
        assertThat(cardStatus).isEqualTo("REVIEWED");
        String idStatus = response.getDocuments().stream()
                .filter(d -> d.getDocumentType().equals("ID_DOCUMENT")).findFirst().orElseThrow().getStatus();
        assertThat(idStatus).isEqualTo("NEEDS_CORRECTION");
    }

    @Test
    void getMyProfile_shouldThrowNotFound_whenNoneExists() {
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile())
                .isInstanceOf(ProfessionalProfileNotFoundException.class);
    }
}
