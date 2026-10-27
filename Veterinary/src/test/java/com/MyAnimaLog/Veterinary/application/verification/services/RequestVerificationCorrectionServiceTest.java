package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RequestVerificationCorrectionRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.RequestVerificationCorrectionResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.CannotReviewOwnRequestException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidDocumentTypeException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidReviewDecisionException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestVerificationCorrectionServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private AuthenticatedUserPort authenticatedUserPort;
    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private RequestVerificationCorrectionService service;

    private UUID requestId;
    private UUID submittedBy;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        submittedBy = UUID.randomUUID();
        adminId = UUID.randomUUID();
        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(adminId, "admin@x.com", true, true, Optional.empty()));
        lenient().when(verificationRequestRepositoryPort.save(any(VerificationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationRequestRepositoryPort.claimForDecision(
                        requestId, VerificationRequestStatus.UNDER_REVIEW, VerificationRequestStatus.NEEDS_CORRECTION))
                .thenReturn(true);
    }

    @Test
    void requestCorrection_shouldMoveClinicToNeedsCorrection() {
        UUID veterinaryId = UUID.randomUUID();
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(veterinaryId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                Veterinary.builder().id(veterinaryId).status(VeterinaryStatus.UNDER_REVIEW).build()));

        RequestVerificationCorrectionResponse response = service.requestCorrection(requestId,
                RequestVerificationCorrectionRequest.builder().notes("Falta el RUT vigente").documentTypes(List.of("RUT")).build());

        assertThat(response.getStatus()).isEqualTo(VerificationRequestStatus.NEEDS_CORRECTION);
        assertThat(response.getCorrectionDocumentTypes()).containsExactly("RUT");
        verify(veterinaryRepositoryPort).save(ArgumentMatchers.argThat(v -> v.getStatus() == VeterinaryStatus.NEEDS_CORRECTION));
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void requestCorrection_shouldMoveProfessionalProfileBackToNotSubmitted() {
        UUID profileId = UUID.randomUUID();
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.PROFESSIONAL).subjectId(profileId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        when(professionalProfileRepositoryPort.findById(profileId)).thenReturn(Optional.of(
                ProfessionalProfile.builder().id(profileId).licenseStatus(LicenseStatus.PENDING).build()));
        when(professionalProfileRepositoryPort.save(any(ProfessionalProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.requestCorrection(requestId,
                RequestVerificationCorrectionRequest.builder().notes("Cédula ilegible").documentTypes(List.of("ID_DOCUMENT")).build());

        verify(professionalProfileRepositoryPort).save(ArgumentMatchers.argThat(p -> p.getLicenseStatus() == LicenseStatus.NOT_SUBMITTED));
    }

    @Test
    void requestCorrection_shouldThrowInvalidReviewDecision_whenNotesMissing() {
        assertThatThrownBy(() -> service.requestCorrection(requestId,
                RequestVerificationCorrectionRequest.builder().documentTypes(List.of("RUT")).build()))
                .isInstanceOf(InvalidReviewDecisionException.class);
    }

    @Test
    void requestCorrection_shouldThrowInvalidReviewDecision_whenDocumentTypesMissing() {
        assertThatThrownBy(() -> service.requestCorrection(requestId,
                RequestVerificationCorrectionRequest.builder().notes("x").documentTypes(List.of()).build()))
                .isInstanceOf(InvalidReviewDecisionException.class);
    }

    @Test
    void requestCorrection_shouldThrowInvalidDocumentType_whenTypeDoesNotBelongToSubject() {
        UUID veterinaryId = UUID.randomUUID();
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(veterinaryId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.requestCorrection(requestId,
                RequestVerificationCorrectionRequest.builder().notes("x").documentTypes(List.of("PROFESSIONAL_CARD")).build()))
                .isInstanceOf(InvalidDocumentTypeException.class);
    }

    @Test
    void requestCorrection_shouldThrowInvalidState_whenNotUnderReview() {
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.DRAFT).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.requestCorrection(requestId,
                RequestVerificationCorrectionRequest.builder().notes("x").documentTypes(List.of("RUT")).build()))
                .isInstanceOf(InvalidVerificationStateException.class);
    }

    @Test
    void requestCorrection_shouldThrowCannotReviewOwnRequest() {
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(adminId).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.requestCorrection(requestId,
                RequestVerificationCorrectionRequest.builder().notes("x").documentTypes(List.of("RUT")).build()))
                .isInstanceOf(CannotReviewOwnRequestException.class);
    }

    @Test
    void requestCorrection_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.requestCorrection(requestId,
                RequestVerificationCorrectionRequest.builder().notes("x").documentTypes(List.of("RUT")).build()))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }
}
