package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RejectVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.RejectVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.CannotReviewOwnRequestException;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RejectVerificationServiceTest {

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
    private RejectVerificationService service;

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
                        requestId, VerificationRequestStatus.UNDER_REVIEW, VerificationRequestStatus.REJECTED))
                .thenReturn(true);
    }

    @Test
    void reject_shouldRejectClinic_andReportAttemptsRemaining() {
        UUID veterinaryId = UUID.randomUUID();
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(veterinaryId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                Veterinary.builder().id(veterinaryId).status(VeterinaryStatus.UNDER_REVIEW).build()));

        RejectVerificationResponse response = service.reject(requestId,
                RejectVerificationRequest.builder().reasonCode(RejectionReasonCode.NOT_IN_REGISTRY).notes("No aparece en RUES").build());

        assertThat(response.getStatus()).isEqualTo(VerificationRequestStatus.REJECTED);
        assertThat(response.getAttemptsRemaining()).isEqualTo(2);
        assertThat(response.getDocumentsDeletionDate()).isNotNull();
        verify(veterinaryRepositoryPort).save(ArgumentMatchers.argThat(v -> v.getStatus() == VeterinaryStatus.REJECTED));
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void reject_shouldRejectProfessionalProfile_andSetRejectionReason() {
        UUID profileId = UUID.randomUUID();
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.PROFESSIONAL).subjectId(profileId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(2).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        when(professionalProfileRepositoryPort.findById(profileId)).thenReturn(Optional.of(
                ProfessionalProfile.builder().id(profileId).licenseStatus(LicenseStatus.PENDING).build()));
        when(professionalProfileRepositoryPort.save(any(ProfessionalProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        RejectVerificationResponse response = service.reject(requestId,
                RejectVerificationRequest.builder().reasonCode(RejectionReasonCode.DOCUMENT_INVALID).notes("Documento ilegible").build());

        assertThat(response.getAttemptsRemaining()).isEqualTo(1);
        verify(professionalProfileRepositoryPort).save(ArgumentMatchers.argThat(p ->
                p.getLicenseStatus() == LicenseStatus.REJECTED && "Documento ilegible".equals(p.getRejectionReason())));
    }

    @Test
    void reject_shouldThrowInvalidReviewDecision_whenReasonCodeMissing() {
        assertThatThrownBy(() -> service.reject(requestId, RejectVerificationRequest.builder().notes("x").build()))
                .isInstanceOf(InvalidReviewDecisionException.class);
    }

    @Test
    void reject_shouldThrowInvalidReviewDecision_whenNotesBlank() {
        assertThatThrownBy(() -> service.reject(requestId,
                RejectVerificationRequest.builder().reasonCode(RejectionReasonCode.OTHER).notes("  ").build()))
                .isInstanceOf(InvalidReviewDecisionException.class);
    }

    @Test
    void reject_shouldThrowInvalidReviewDecision_whenNotesTooLong() {
        String tooLong = "a".repeat(1001);
        assertThatThrownBy(() -> service.reject(requestId,
                RejectVerificationRequest.builder().reasonCode(RejectionReasonCode.OTHER).notes(tooLong).build()))
                .isInstanceOf(InvalidReviewDecisionException.class);
    }

    @Test
    void reject_shouldThrowInvalidState_whenNotUnderReview() {
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.DRAFT).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.reject(requestId,
                RejectVerificationRequest.builder().reasonCode(RejectionReasonCode.OTHER).notes("x").build()))
                .isInstanceOf(InvalidVerificationStateException.class);
    }

    @Test
    void reject_shouldThrowCannotReviewOwnRequest() {
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(adminId).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.reject(requestId,
                RejectVerificationRequest.builder().reasonCode(RejectionReasonCode.OTHER).notes("x").build()))
                .isInstanceOf(CannotReviewOwnRequestException.class);
    }

    @Test
    void reject_shouldThrowInvalidState_whenAnotherAdminAlreadyDecided() {
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        when(verificationRequestRepositoryPort.claimForDecision(
                requestId, VerificationRequestStatus.UNDER_REVIEW, VerificationRequestStatus.REJECTED)).thenReturn(false);

        assertThatThrownBy(() -> service.reject(requestId,
                RejectVerificationRequest.builder().reasonCode(RejectionReasonCode.OTHER).notes("x").build()))
                .isInstanceOf(InvalidVerificationStateException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void reject_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.reject(requestId,
                RejectVerificationRequest.builder().reasonCode(RejectionReasonCode.OTHER).notes("x").build()))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }
}
