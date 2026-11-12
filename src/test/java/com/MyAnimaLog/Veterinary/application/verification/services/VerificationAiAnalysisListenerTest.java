package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.verification.dto.VerificationSubmittedEvent;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.IaVerificationPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiRiskLevel;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationAiAnalysisListenerTest {

    @Mock
    private IaVerificationPort iaVerificationPort;
    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    @Mock
    private FileStoragePort fileStoragePort;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    private StorageProperties storageProperties;

    private VerificationAiAnalysisListener listener;

    private UUID requestId;
    private UUID subjectId;
    private VerificationRequest request;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        listener = new VerificationAiAnalysisListener(iaVerificationPort, verificationRequestRepositoryPort,
                verificationDocumentRepositoryPort, fileStoragePort, storageProperties, veterinaryRepositoryPort);

        requestId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(subjectId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).aiStatus(AiStatus.NOT_RUN).build();

        lenient().when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        lenient().when(verificationRequestRepositoryPort.save(any(VerificationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationDocumentRepositoryPort.findByRequestId(requestId)).thenReturn(List.of());
        lenient().when(veterinaryRepositoryPort.findById(subjectId)).thenReturn(Optional.of(
                Veterinary.builder().id(subjectId).name("El Bosque").city("Cartagena")
                        .legalName("El Bosque SAS").legalRepName("Ana Pérez")
                        .nit(Nit.fromPersisted("900123456-1")).tenantId(UUID.randomUUID().toString())
                        .status(VeterinaryStatus.ACTIVE).active(true).build()));
    }

    @Test
    void onVerificationSubmitted_shouldDoNothing_whenAiDisabled() {
        when(iaVerificationPort.isEnabled()).thenReturn(false);

        listener.onVerificationSubmitted(new VerificationSubmittedEvent(requestId));

        verify(verificationRequestRepositoryPort, never()).findById(any());
        verify(verificationRequestRepositoryPort, never()).save(any());
    }

    @Test
    void onVerificationSubmitted_shouldSetRunningThenDone_withReportAndRiskLevel() {
        when(iaVerificationPort.isEnabled()).thenReturn(true);
        VerificationDocument doc = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(requestId).documentType("RUT")
                .storagePath("verifications/" + requestId + "/doc.pdf").build();
        when(verificationDocumentRepositoryPort.findByRequestId(requestId)).thenReturn(List.of(doc));
        when(fileStoragePort.createSignedUrl(anyString(), anyString(), any(Duration.class)))
                .thenReturn("https://signed.example/doc.pdf");
        when(iaVerificationPort.analyze(any())).thenReturn(
                new IaVerificationPort.IaAnalysisResult("READY_FOR_HUMAN_REVIEW", "LOW", "{\"ok\":true}"));

        listener.onVerificationSubmitted(new VerificationSubmittedEvent(requestId));

        verify(verificationRequestRepositoryPort).save(ArgumentMatchers.argThat(r -> r.getAiStatus() == AiStatus.RUNNING));
        verify(verificationRequestRepositoryPort).save(ArgumentMatchers.argThat(r ->
                r.getAiStatus() == AiStatus.DONE && r.getAiRiskLevel() == AiRiskLevel.LOW && "{\"ok\":true}".equals(r.getAiReport())));
    }

    @Test
    void onVerificationSubmitted_shouldStillCompleteAsDone_whenRecommendationIsUnexpected() {
        when(iaVerificationPort.isEnabled()).thenReturn(true);
        when(iaVerificationPort.analyze(any())).thenReturn(
                new IaVerificationPort.IaAnalysisResult("SOMETHING_ELSE", "MEDIUM", "{}"));

        listener.onVerificationSubmitted(new VerificationSubmittedEvent(requestId));

        verify(verificationRequestRepositoryPort).save(ArgumentMatchers.argThat(r -> r.getAiStatus() == AiStatus.DONE));
    }

    @Test
    void onVerificationSubmitted_shouldSetFailed_whenAnalyzeThrows() {
        when(iaVerificationPort.isEnabled()).thenReturn(true);
        when(iaVerificationPort.analyze(any())).thenThrow(new RuntimeException("IA down"));

        listener.onVerificationSubmitted(new VerificationSubmittedEvent(requestId));

        verify(verificationRequestRepositoryPort).save(ArgumentMatchers.argThat(r -> r.getAiStatus() == AiStatus.FAILED));
    }

    @Test
    void onVerificationSubmitted_shouldSendDeclaredClinicData_forClinicSubjects() {
        when(iaVerificationPort.isEnabled()).thenReturn(true);
        when(iaVerificationPort.analyze(any())).thenReturn(
                new IaVerificationPort.IaAnalysisResult("READY_FOR_HUMAN_REVIEW", "LOW", "{}"));

        listener.onVerificationSubmitted(new VerificationSubmittedEvent(requestId));

        verify(iaVerificationPort).analyze(ArgumentMatchers.argThat(r ->
                r.declared() != null
                        && "El Bosque SAS".equals(r.declared().clinicName())
                        && "900123456-1".equals(r.declared().nit())
                        && "Ana Pérez".equals(r.declared().legalRepresentativeName())
                        && "Cartagena".equals(r.declared().city())));
    }

    @Test
    void onVerificationSubmitted_shouldNotSendDeclaredData_forProfessionalSubjects() {
        VerificationRequest professionalRequest = request.toBuilder()
                .subjectType(SubjectType.PROFESSIONAL).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(professionalRequest));
        when(iaVerificationPort.isEnabled()).thenReturn(true);
        when(iaVerificationPort.analyze(any())).thenReturn(
                new IaVerificationPort.IaAnalysisResult("READY_FOR_HUMAN_REVIEW", "LOW", "{}"));

        listener.onVerificationSubmitted(new VerificationSubmittedEvent(requestId));

        verify(iaVerificationPort).analyze(ArgumentMatchers.argThat(r -> r.declared() == null));
        verify(veterinaryRepositoryPort, never()).findById(any());
    }

    @Test
    void onVerificationSubmitted_shouldDoNothing_whenRequestNoLongerExists() {
        when(iaVerificationPort.isEnabled()).thenReturn(true);
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.empty());

        listener.onVerificationSubmitted(new VerificationSubmittedEvent(requestId));

        verify(verificationRequestRepositoryPort, never()).save(any());
        verify(iaVerificationPort, never()).analyze(any());
    }
}
