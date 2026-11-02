package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationDetailResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiRiskLevel;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAdminVerificationDetailServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private UserServicePort userServicePort;
    @Mock
    private FileStoragePort fileStoragePort;

    private StorageProperties storageProperties;
    private GetAdminVerificationDetailService service;

    private UUID requestId;
    private UUID veterinaryId;
    private UUID submittedBy;
    private VerificationRequest clinicRequest;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        service = new GetAdminVerificationDetailService(authorizationService, verificationRequestRepositoryPort,
                verificationDocumentRepositoryPort, veterinaryRepositoryPort, professionalProfileRepositoryPort,
                employeeRepositoryPort, userServicePort, fileStoragePort, storageProperties, new ObjectMapper());
        ReflectionTestUtils.setField(service, "ruesUrl", "https://www.rues.org.co/");
        ReflectionTestUtils.setField(service, "comvezcolUrl", "https://consejoprofesionalmvz.gov.co/consulta-de-profesionales/");

        requestId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        submittedBy = UUID.randomUUID();
        clinicRequest = VerificationRequest.builder()
                .id(requestId)
                .subjectType(SubjectType.CLINIC)
                .subjectId(veterinaryId)
                .status(VerificationRequestStatus.UNDER_REVIEW)
                .attemptNumber(1)
                .submittedBy(submittedBy)
                .aiStatus(AiStatus.NOT_RUN)
                .build();

        lenient().when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(clinicRequest));
        lenient().when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                Veterinary.builder().id(veterinaryId).name("Clinica Test").nit(Nit.fromPersisted("900123456-1")).build()));
        lenient().when(verificationDocumentRepositoryPort.findByRequestId(requestId)).thenReturn(List.of());
        lenient().when(verificationRequestRepositoryPort.findBySubjectTypeAndSubjectId(SubjectType.CLINIC, veterinaryId))
                .thenReturn(List.of(clinicRequest));
    }

    @Test
    void getDetail_shouldIncludeSignedUrlPerDocument() {
        VerificationDocument doc = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(requestId).documentType("RUT")
                .storagePath("verifications/" + requestId + "/doc.pdf").mimeType("application/pdf")
                .fileSizeBytes(100L).sha256("abc").uploadedAt(LocalDateTime.now()).build();
        when(verificationDocumentRepositoryPort.findByRequestId(requestId)).thenReturn(List.of(doc));
        when(verificationDocumentRepositoryPort.findBySha256("abc")).thenReturn(List.of(doc));
        when(fileStoragePort.createSignedUrl(eq(storageProperties.getVerificationBucket()), eq(doc.getStoragePath()), any(Duration.class)))
                .thenReturn("https://signed.example/doc.pdf");
        when(userServicePort.getUser(submittedBy)).thenReturn(new UserServicePort.UserInfo(submittedBy, "owner@x.com", true, null));

        AdminVerificationDetailResponse response = service.getDetail(requestId);

        assertThat(response.getDocuments()).hasSize(1);
        assertThat(response.getDocuments().get(0).getSignedUrl()).isEqualTo("https://signed.example/doc.pdf");
        assertThat(response.getDocuments().get(0).getSignedUrlExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(response.getAccount().getEmail()).isEqualTo("owner@x.com");
        assertThat(response.getRegistryLookupUrl()).isEqualTo("https://www.rues.org.co/");
        assertThat(response.getClinic().getName()).isEqualTo("Clinica Test");
        assertThat(response.getClinic().getNit()).isEqualTo("900123456-1");
    }

    @Test
    void getDetail_shouldFallBackToIdOnlyAccount_whenUserServiceFails() {
        when(userServicePort.getUser(submittedBy)).thenThrow(new ExternalServiceUnavailableException("down"));

        AdminVerificationDetailResponse response = service.getDetail(requestId);

        assertThat(response.getAccount().getId()).isEqualTo(submittedBy);
        assertThat(response.getAccount().getEmail()).isNull();
    }

    @Test
    void getDetail_shouldResolveProfessionalWorkplaces() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherVetId = UUID.randomUUID();
        VerificationRequest professionalRequest = clinicRequest.toBuilder()
                .subjectType(SubjectType.PROFESSIONAL).subjectId(profileId).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(professionalRequest));
        when(verificationRequestRepositoryPort.findBySubjectTypeAndSubjectId(SubjectType.PROFESSIONAL, profileId))
                .thenReturn(List.of(professionalRequest));
        when(professionalProfileRepositoryPort.findById(profileId)).thenReturn(Optional.of(
                ProfessionalProfile.builder().id(profileId).userId(userId).fullName("Juan Pérez").profession(Profession.MV).build()));
        when(employeeRepositoryPort.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(
                VeterinaryEmployee.builder().veterinaryId(otherVetId).role(EmployeeRole.VETERINARIAN).active(true).build()));
        when(veterinaryRepositoryPort.findById(otherVetId)).thenReturn(Optional.of(
                Veterinary.builder().id(otherVetId).name("Otra Clinica").build()));
        when(userServicePort.getUser(submittedBy)).thenReturn(new UserServicePort.UserInfo(submittedBy, "vet@x.com", true, null));

        AdminVerificationDetailResponse response = service.getDetail(requestId);

        assertThat(response.getProfessional().getWorkplaces()).hasSize(1);
        assertThat(response.getProfessional().getWorkplaces().get(0).getName()).isEqualTo("Otra Clinica");
        assertThat(response.getRegistryLookupUrl()).isEqualTo("https://consejoprofesionalmvz.gov.co/consulta-de-profesionales/");
    }

    @Test
    void getDetail_shouldParseAiReportAndFlagDuplicates() {
        VerificationDocument doc = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(requestId).documentType("RUT")
                .storagePath("verifications/" + requestId + "/doc.pdf").sha256("dup-hash")
                .uploadedAt(LocalDateTime.now()).build();
        UUID otherVetId = UUID.randomUUID();
        VerificationRequest otherRequest = VerificationRequest.builder()
                .id(UUID.randomUUID()).subjectType(SubjectType.CLINIC).subjectId(otherVetId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).build();
        VerificationDocument otherDoc = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(otherRequest.getId()).documentType("RUT")
                .storagePath("verifications/other/doc.pdf").sha256("dup-hash").uploadedAt(LocalDateTime.now()).build();

        VerificationRequest withReport = clinicRequest.toBuilder()
                .aiStatus(AiStatus.DONE).aiRiskLevel(AiRiskLevel.LOW)
                .aiReport("{\"summaryForReviewer\":\"ok\",\"crossChecks\":[],\"documents\":[{\"documentType\":\"RUT\",\"issueDate\":\"2026-01-01\"}]}")
                .build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(withReport));
        when(verificationDocumentRepositoryPort.findByRequestId(requestId)).thenReturn(List.of(doc));
        when(verificationDocumentRepositoryPort.findBySha256("dup-hash")).thenReturn(List.of(doc, otherDoc));
        when(verificationRequestRepositoryPort.findById(otherRequest.getId())).thenReturn(Optional.of(otherRequest));
        when(veterinaryRepositoryPort.findById(otherVetId)).thenReturn(Optional.of(Veterinary.builder().id(otherVetId).name("Otra").build()));
        when(fileStoragePort.createSignedUrl(eq(storageProperties.getVerificationBucket()), eq(doc.getStoragePath()), any(Duration.class)))
                .thenReturn("https://signed.example/doc.pdf");
        when(userServicePort.getUser(submittedBy)).thenReturn(new UserServicePort.UserInfo(submittedBy, "owner@x.com", true, null));

        AdminVerificationDetailResponse response = service.getDetail(requestId);

        assertThat(response.getAi().getReport()).containsEntry("summaryForReviewer", "ok");
        assertThat(response.getDocuments().get(0).getIssueDate().toString()).isEqualTo("2026-01-01");
        assertThat(response.getDuplicates()).hasSize(1);
        assertThat(response.getDuplicates().get(0).getOtherSubjectName()).isEqualTo("Otra");
    }

    @Test
    void getDetail_shouldIncludeHistoryExcludingCurrentRequest() {
        VerificationRequest previousRejected = VerificationRequest.builder()
                .id(UUID.randomUUID()).subjectType(SubjectType.CLINIC).subjectId(veterinaryId)
                .status(VerificationRequestStatus.REJECTED).attemptNumber(1)
                .rejectionReasonCode(RejectionReasonCode.DATA_MISMATCH).reviewerNotes("mismatch").build();
        when(verificationRequestRepositoryPort.findBySubjectTypeAndSubjectId(SubjectType.CLINIC, veterinaryId))
                .thenReturn(List.of(clinicRequest, previousRejected));
        when(userServicePort.getUser(submittedBy)).thenReturn(new UserServicePort.UserInfo(submittedBy, "owner@x.com", true, null));

        AdminVerificationDetailResponse response = service.getDetail(requestId);

        assertThat(response.getHistory()).hasSize(1);
        assertThat(response.getHistory().get(0).getRejectionReasonCode()).isEqualTo(RejectionReasonCode.DATA_MISMATCH);
    }

    @Test
    void getDetail_shouldThrowVerificationRequestNotFound_whenMissing() {
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(requestId))
                .isInstanceOf(VerificationRequestNotFoundException.class);
    }

    @Test
    void getDetail_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.getDetail(requestId))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }

}
