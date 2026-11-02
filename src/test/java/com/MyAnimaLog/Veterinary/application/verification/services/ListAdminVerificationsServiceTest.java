package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationQueueItem;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationQueueResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAdminVerificationsServiceTest {

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

    @InjectMocks
    private ListAdminVerificationsService service;

    private UUID veterinaryId;
    private VerificationRequest clinicRequest;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        clinicRequest = VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(SubjectType.CLINIC)
                .subjectId(veterinaryId)
                .status(VerificationRequestStatus.UNDER_REVIEW)
                .attemptNumber(1)
                .submittedAt(LocalDateTime.now())
                .build();

        lenient().when(verificationDocumentRepositoryPort.findByRequestId(clinicRequest.getId())).thenReturn(List.of());
        lenient().when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                Veterinary.builder().id(veterinaryId).name("Clinica Test").nit(Nit.fromPersisted("900123456-1")).build()));
    }

    @Test
    void list_shouldDefaultToUnderReview_whenStatusNotProvided() {
        when(verificationRequestRepositoryPort.findForAdminQueue(VerificationRequestStatus.UNDER_REVIEW, null, 0, 20))
                .thenReturn(new VerificationRequestRepositoryPort.AdminQueuePage(List.of(clinicRequest), 1));
        when(verificationRequestRepositoryPort.countByStatusForAdminQueue(null)).thenReturn(Map.of());

        AdminVerificationQueueResponse response = service.list(null, null, null, null);

        assertThat(response.getItems()).hasSize(1);
        AdminVerificationQueueItem item = response.getItems().get(0);
        assertThat(item.getDisplayName()).isEqualTo("Clinica Test");
        assertThat(item.getIdentifier()).isEqualTo("900123456-1");
        assertThat(item.getDuplicateFileAlert()).isFalse();
        assertThat(response.getCounts()).containsKeys("UNDER_REVIEW", "NEEDS_CORRECTION", "APPROVED", "REJECTED");
        assertThat(response.getCounts().get("UNDER_REVIEW")).isZero();
    }

    @Test
    void list_shouldRespectExplicitStatusSubjectTypePageAndSize() {
        when(verificationRequestRepositoryPort.findForAdminQueue(VerificationRequestStatus.REJECTED, SubjectType.PROFESSIONAL, 2, 5))
                .thenReturn(new VerificationRequestRepositoryPort.AdminQueuePage(List.of(), 0));
        when(verificationRequestRepositoryPort.countByStatusForAdminQueue(SubjectType.PROFESSIONAL)).thenReturn(Map.of());

        AdminVerificationQueueResponse response = service.list(VerificationRequestStatus.REJECTED, SubjectType.PROFESSIONAL, 2, 5);

        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void list_shouldResolveProfessionalDisplayNameIdentifierAndProfession() {
        UUID profileId = UUID.randomUUID();
        VerificationRequest professionalRequest = VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(SubjectType.PROFESSIONAL)
                .subjectId(profileId)
                .status(VerificationRequestStatus.UNDER_REVIEW)
                .attemptNumber(1)
                .build();
        when(verificationRequestRepositoryPort.findForAdminQueue(VerificationRequestStatus.UNDER_REVIEW, null, 0, 20))
                .thenReturn(new VerificationRequestRepositoryPort.AdminQueuePage(List.of(professionalRequest), 1));
        when(verificationRequestRepositoryPort.countByStatusForAdminQueue(null)).thenReturn(Map.of());
        when(verificationDocumentRepositoryPort.findByRequestId(professionalRequest.getId())).thenReturn(List.of());
        when(professionalProfileRepositoryPort.findById(profileId)).thenReturn(Optional.of(
                ProfessionalProfile.builder().id(profileId).fullName("Juan Pérez").licenseNumber("TP-123").profession(Profession.MV).build()));

        AdminVerificationQueueResponse response = service.list(null, null, null, null);

        AdminVerificationQueueItem item = response.getItems().get(0);
        assertThat(item.getDisplayName()).isEqualTo("Juan Pérez");
        assertThat(item.getIdentifier()).isEqualTo("TP-123");
        assertThat(item.getProfession()).isEqualTo("MV");
    }

    @Test
    void list_shouldFlagDuplicateFileAlert_whenSameSha256BelongsToAnotherSubject() {
        VerificationDocument ownDoc = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(clinicRequest.getId()).documentType("RUT")
                .storagePath("verifications/a/doc.pdf").sha256("abc").uploadedAt(LocalDateTime.now()).build();
        UUID otherVetId = UUID.randomUUID();
        VerificationRequest otherRequest = VerificationRequest.builder()
                .id(UUID.randomUUID()).subjectType(SubjectType.CLINIC).subjectId(otherVetId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).build();
        VerificationDocument otherDoc = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(otherRequest.getId()).documentType("RUT")
                .storagePath("verifications/b/doc.pdf").sha256("abc").uploadedAt(LocalDateTime.now()).build();

        when(verificationRequestRepositoryPort.findForAdminQueue(VerificationRequestStatus.UNDER_REVIEW, null, 0, 20))
                .thenReturn(new VerificationRequestRepositoryPort.AdminQueuePage(List.of(clinicRequest), 1));
        when(verificationRequestRepositoryPort.countByStatusForAdminQueue(null)).thenReturn(Map.of());
        when(verificationDocumentRepositoryPort.findByRequestId(clinicRequest.getId())).thenReturn(List.of(ownDoc));
        when(verificationDocumentRepositoryPort.findBySha256("abc")).thenReturn(List.of(ownDoc, otherDoc));
        when(verificationRequestRepositoryPort.findById(otherRequest.getId())).thenReturn(Optional.of(otherRequest));

        AdminVerificationQueueResponse response = service.list(null, null, null, null);

        assertThat(response.getItems().get(0).getDuplicateFileAlert()).isTrue();
    }

    @Test
    void list_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.list(null, null, null, null))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }
}
