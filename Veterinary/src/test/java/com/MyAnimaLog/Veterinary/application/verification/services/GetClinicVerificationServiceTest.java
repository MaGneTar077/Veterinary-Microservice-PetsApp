package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.GetClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetClinicVerificationServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private GetClinicVerificationService service;

    private UUID veterinaryId;
    private UUID callerId;
    private Veterinary veterinary;
    private VerificationRequest request;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        callerId = UUID.randomUUID();
        veterinary = Veterinary.builder().id(veterinaryId).build();
        request = VerificationRequest.builder()
                .id(UUID.randomUUID())
                .status(VerificationRequestStatus.DRAFT)
                .attemptNumber(1)
                .build();

        lenient().when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        lenient().when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(request));
        lenient().when(verificationDocumentRepositoryPort.findByRequestId(request.getId())).thenReturn(List.of());
        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(callerId, "owner@example.com", true, false, Optional.empty()));
        lenient().when(professionalProfileRepositoryPort.findByUserId(callerId)).thenReturn(Optional.empty());
    }

    @Test
    void getVerification_shouldMarkAllRequiredDocumentsAsPending_whenNoneUploaded() {
        GetClinicVerificationResponse response = service.getVerification(veterinaryId);

        assertThat(response.getMissingRequired()).containsExactlyInAnyOrder("RUT", "CHAMBER_OF_COMMERCE", "LEGAL_REP_ID");
        assertThat(response.getDocuments()).hasSize(4);
        assertThat(response.getCanEdit()).isTrue();
        assertThat(response.getCanSubmit()).isFalse();
        assertThat(response.getAttemptsRemaining()).isEqualTo(2);
        assertThat(response.getMaxAttempts()).isEqualTo(3);
    }

    @Test
    void getVerification_shouldMarkDocumentReady_whenUploadedAndNotFlaggedForCorrection() {
        VerificationDocument rut = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(request.getId()).documentType("RUT")
                .storagePath("verifications/req/doc.pdf").uploadedAt(LocalDateTime.now()).build();
        when(verificationDocumentRepositoryPort.findByRequestId(request.getId())).thenReturn(List.of(rut));

        GetClinicVerificationResponse response = service.getVerification(veterinaryId);

        String rutStatus = response.getDocuments().stream()
                .filter(d -> d.getDocumentType().equals("RUT")).findFirst().orElseThrow().getStatus();
        assertThat(rutStatus).isEqualTo("READY");
        assertThat(response.getMissingRequired()).containsExactlyInAnyOrder("CHAMBER_OF_COMMERCE", "LEGAL_REP_ID");
    }

    @Test
    void getVerification_shouldMarkDocumentNeedsCorrection_whenFlaggedAndUploadedBeforeCorrectionRequest() {
        LocalDateTime uploadedAt = LocalDateTime.now().minusDays(2);
        LocalDateTime correctionRequestedAt = LocalDateTime.now().minusDays(1);
        VerificationRequest needsCorrection = request.toBuilder()
                .status(VerificationRequestStatus.NEEDS_CORRECTION)
                .correctionDocumentTypes(List.of("RUT"))
                .correctionRequestedAt(correctionRequestedAt)
                .build();
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(needsCorrection));
        VerificationDocument rut = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(needsCorrection.getId()).documentType("RUT")
                .storagePath("verifications/req/doc.pdf").uploadedAt(uploadedAt).build();
        when(verificationDocumentRepositoryPort.findByRequestId(needsCorrection.getId())).thenReturn(List.of(rut));

        GetClinicVerificationResponse response = service.getVerification(veterinaryId);

        String rutStatus = response.getDocuments().stream()
                .filter(d -> d.getDocumentType().equals("RUT")).findFirst().orElseThrow().getStatus();
        assertThat(rutStatus).isEqualTo("NEEDS_CORRECTION");
    }

    @Test
    void getVerification_shouldAllowSubmit_whenLegalRepCompleteAndAllRequiredDocumentsPresent() {
        Veterinary complete = veterinary.toBuilder()
                .legalRepIsOwner(true).legalRepName("Juan").legalRepDocumentType("CC").legalRepDocumentNumber("123")
                .build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(complete));
        List<VerificationDocument> docs = List.of(
                doc("RUT", request.getId()), doc("CHAMBER_OF_COMMERCE", request.getId()), doc("LEGAL_REP_ID", request.getId()));
        when(verificationDocumentRepositoryPort.findByRequestId(request.getId())).thenReturn(docs);

        GetClinicVerificationResponse response = service.getVerification(veterinaryId);

        assertThat(response.getCanSubmit()).isTrue();
        assertThat(response.getMissingRequired()).isEmpty();
        assertThat(response.getLegalRepresentative().getFullName()).isEqualTo("Juan");
    }

    @Test
    void getVerification_shouldReportLicenseStatus_whenCallerHasProfessionalProfile() {
        when(professionalProfileRepositoryPort.findByUserId(callerId)).thenReturn(Optional.of(
                ProfessionalProfile.builder().userId(callerId).licenseStatus(LicenseStatus.PENDING).build()));

        GetClinicVerificationResponse response = service.getVerification(veterinaryId);

        assertThat(response.getProfessionalProfile().getExists()).isTrue();
        assertThat(response.getProfessionalProfile().getLicenseStatus()).isEqualTo(LicenseStatus.PENDING);
    }

    @Test
    void getVerification_shouldMarkUnrequestedDocumentReviewed_whenNeedsCorrection() {
        LocalDateTime uploadedAt = LocalDateTime.now().minusDays(2);
        LocalDateTime correctionRequestedAt = LocalDateTime.now().minusDays(1);
        VerificationRequest needsCorrection = request.toBuilder()
                .status(VerificationRequestStatus.NEEDS_CORRECTION)
                .correctionDocumentTypes(List.of("RUT"))
                .correctionRequestedAt(correctionRequestedAt)
                .build();
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(needsCorrection));
        VerificationDocument chamberOfCommerce = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(needsCorrection.getId()).documentType("CHAMBER_OF_COMMERCE")
                .storagePath("verifications/req/doc.pdf").uploadedAt(uploadedAt).build();
        when(verificationDocumentRepositoryPort.findByRequestId(needsCorrection.getId())).thenReturn(List.of(chamberOfCommerce));

        GetClinicVerificationResponse response = service.getVerification(veterinaryId);

        String status = response.getDocuments().stream()
                .filter(d -> d.getDocumentType().equals("CHAMBER_OF_COMMERCE")).findFirst().orElseThrow().getStatus();
        assertThat(status).isEqualTo("REVIEWED");
    }

    @Test
    void getVerification_shouldExposeRejectionReasonCode_whenRejected() {
        VerificationRequest rejected = request.toBuilder()
                .status(VerificationRequestStatus.REJECTED)
                .rejectionReasonCode(RejectionReasonCode.NOT_IN_REGISTRY)
                .build();
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(rejected));

        GetClinicVerificationResponse response = service.getVerification(veterinaryId);

        assertThat(response.getRejectionReasonCode()).isEqualTo(RejectionReasonCode.NOT_IN_REGISTRY);
    }

    @Test
    void getVerification_shouldComputeDocumentsDeletionDate_whenRejected() {
        LocalDateTime decidedAt = LocalDateTime.now();
        VerificationRequest rejected = request.toBuilder().status(VerificationRequestStatus.REJECTED).decidedAt(decidedAt).build();
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(rejected));

        GetClinicVerificationResponse response = service.getVerification(veterinaryId);

        assertThat(response.getDocumentsDeletionDate()).isEqualTo(decidedAt.plusDays(90));
        assertThat(response.getCanEdit()).isFalse();
    }

    @Test
    void getVerification_shouldThrowVerificationRequestNotFound_whenNoneExists() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVerification(veterinaryId))
                .isInstanceOf(VerificationRequestNotFoundException.class);
    }

    @Test
    void getVerification_shouldPropagateNotVeterinaryOwner() {
        doThrow(new NotVeterinaryOwnerException()).when(authorizationService).requireVeterinaryOwner(veterinaryId);

        assertThatThrownBy(() -> service.getVerification(veterinaryId))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    private VerificationDocument doc(String type, UUID requestId) {
        return VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(requestId).documentType(type)
                .storagePath("verifications/" + requestId + "/" + UUID.randomUUID() + ".pdf")
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
