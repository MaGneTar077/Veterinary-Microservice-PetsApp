package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.dto.SubmitProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.services.ProfessionalProfileSubmissionService;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.MissingRequiredDocumentsException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.TruthfulnessDeclarationRequiredException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitClinicVerificationServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private ProfessionalProfileSubmissionService professionalProfileSubmissionService;

    @InjectMocks
    private SubmitClinicVerificationService service;

    private UUID veterinaryId;
    private Veterinary completeVeterinary;
    private VerificationRequest draftRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "supportEmail", "support@myanimalog.com");

        veterinaryId = UUID.randomUUID();
        completeVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .legalRepIsOwner(true).legalRepName("Juan").legalRepDocumentType("CC").legalRepDocumentNumber("123")
                .build();
        draftRequest = VerificationRequest.builder().id(UUID.randomUUID()).status(VerificationRequestStatus.DRAFT)
                .attemptNumber(1).build();

        lenient().when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(completeVeterinary));
        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(draftRequest));
        lenient().when(verificationRequestRepositoryPort.save(any(VerificationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationDocumentRepositoryPort.findByRequestId(draftRequest.getId()))
                .thenReturn(List.of(doc("RUT"), doc("CHAMBER_OF_COMMERCE"), doc("LEGAL_REP_ID")));
    }

    private VerificationDocument doc(String type) {
        return VerificationDocument.builder().id(UUID.randomUUID()).requestId(draftRequest.getId()).documentType(type).build();
    }

    private SubmitClinicVerificationRequest requestBody(boolean declared) {
        return SubmitClinicVerificationRequest.builder().truthfulnessDeclared(declared).build();
    }

    private SubmitClinicVerificationRequest requestBodyWithProfile(boolean declared) {
        return SubmitClinicVerificationRequest.builder().truthfulnessDeclared(declared).includeProfessionalProfile(true).build();
    }

    @Test
    void submit_shouldTransitionToUnderReview_whenEverythingIsComplete() {
        SubmitClinicVerificationResponse response = service.submit(veterinaryId, requestBody(true));

        assertThat(response.getStatus()).isEqualTo(VerificationRequestStatus.UNDER_REVIEW);
        assertThat(response.getSubmittedAt()).isNotNull();
        verify(veterinaryRepositoryPort).save(argThat((Veterinary v) -> v.getStatus() == VeterinaryStatus.UNDER_REVIEW));
        verify(eventPublisherPort, times(1)).publish(any());
    }

    @Test
    void submit_shouldClearCorrectionFields_whenResubmittingFromNeedsCorrection() {
        VerificationRequest needsCorrection = draftRequest.toBuilder()
                .status(VerificationRequestStatus.NEEDS_CORRECTION)
                .correctionDocumentTypes(List.of("RUT"))
                .correctionRequestedAt(LocalDateTime.now())
                .build();
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(needsCorrection));
        when(verificationDocumentRepositoryPort.findByRequestId(needsCorrection.getId()))
                .thenReturn(List.of(doc("RUT"), doc("CHAMBER_OF_COMMERCE"), doc("LEGAL_REP_ID")));

        service.submit(veterinaryId, requestBody(true));

        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getCorrectionDocumentTypes()).isNull();
        assertThat(captor.getValue().getCorrectionRequestedAt()).isNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(VerificationRequestStatus.UNDER_REVIEW);
    }

    @Test
    void submit_shouldThrowTruthfulnessDeclarationRequired_whenNotTrue() {
        assertThatThrownBy(() -> service.submit(veterinaryId, requestBody(false)))
                .isInstanceOf(TruthfulnessDeclarationRequiredException.class);

        verify(verificationRequestRepositoryPort, never()).save(any());
    }

    @Test
    void submit_shouldThrowTruthfulnessDeclarationRequired_whenNull() {
        assertThatThrownBy(() -> service.submit(veterinaryId, requestBody(false)))
                .isInstanceOf(TruthfulnessDeclarationRequiredException.class);
    }

    @Test
    void submit_shouldThrowMissingRequiredDocuments_whenADocumentIsMissing() {
        when(verificationDocumentRepositoryPort.findByRequestId(draftRequest.getId())).thenReturn(List.of(doc("RUT")));

        assertThatThrownBy(() -> service.submit(veterinaryId, requestBody(true)))
                .isInstanceOf(MissingRequiredDocumentsException.class);

        verify(verificationRequestRepositoryPort, never()).save(any());
    }

    @Test
    void submit_shouldThrowMissingRequiredDocuments_whenLegalRepresentativeIsIncomplete() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder().id(veterinaryId).build()));

        assertThatThrownBy(() -> service.submit(veterinaryId, requestBody(true)))
                .isInstanceOf(MissingRequiredDocumentsException.class);
    }

    @Test
    void submit_shouldThrowInvalidVerificationState_whenRequestIsUnderReview() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(draftRequest.toBuilder().status(VerificationRequestStatus.UNDER_REVIEW).build()));

        assertThatThrownBy(() -> service.submit(veterinaryId, requestBody(true)))
                .isInstanceOf(InvalidVerificationStateException.class);
    }

    @Test
    void submit_shouldPropagateNotVeterinaryOwner() {
        doThrow(new NotVeterinaryOwnerException()).when(authorizationService).requireVeterinaryOwner(veterinaryId);

        assertThatThrownBy(() -> service.submit(veterinaryId, requestBody(true)))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    @Test
    void submit_shouldAlsoSubmitProfessionalProfile_whenIncludeProfessionalProfileIsTrue() {
        UUID callerId = UUID.randomUUID();
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(callerId, "owner@example.com", true, false, java.util.Optional.empty()));
        when(professionalProfileSubmissionService.submitForUser(callerId))
                .thenReturn(SubmitProfessionalProfileResponse.builder().build());

        service.submit(veterinaryId, requestBodyWithProfile(true));

        verify(professionalProfileSubmissionService, times(1)).submitForUser(callerId);
        verify(veterinaryRepositoryPort).save(argThat((Veterinary v) -> v.getStatus() == VeterinaryStatus.UNDER_REVIEW));
    }

    @Test
    void submit_shouldFailEntirely_whenProfessionalProfileIsNotReady() {
        UUID callerId = UUID.randomUUID();
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(callerId, "owner@example.com", true, false, java.util.Optional.empty()));
        when(professionalProfileSubmissionService.submitForUser(callerId))
                .thenThrow(new MissingRequiredDocumentsException("Missing required: PROFESSIONAL_CARD"));

        assertThatThrownBy(() -> service.submit(veterinaryId, requestBodyWithProfile(true)))
                .isInstanceOf(MissingRequiredDocumentsException.class);

        // The clinic's own veterinary-status update and event never happen either — since this
        // all runs inside the same @Transactional method, Spring rolls the whole thing back,
        // including the verification_request save that already ran before this check.
        verify(veterinaryRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void submit_shouldFailEntirely_whenCallerHasNoProfessionalProfile() {
        UUID callerId = UUID.randomUUID();
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(callerId, "owner@example.com", true, false, java.util.Optional.empty()));
        when(professionalProfileSubmissionService.submitForUser(callerId))
                .thenThrow(new ProfessionalProfileNotFoundException());

        assertThatThrownBy(() -> service.submit(veterinaryId, requestBodyWithProfile(true)))
                .isInstanceOf(ProfessionalProfileNotFoundException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}
