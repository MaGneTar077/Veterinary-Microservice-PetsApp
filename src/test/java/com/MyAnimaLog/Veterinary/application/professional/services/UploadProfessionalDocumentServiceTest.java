package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.UploadProfessionalDocumentResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.DocumentNotInCorrectionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadProfessionalDocumentServiceTest {

    private static final byte[] PDF_BYTES = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};
    private static final byte[] NOT_A_FILE = {1, 2, 3, 4};

    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private FileStoragePort fileStoragePort;

    private UploadProfessionalDocumentService service;
    private UUID userId;
    private ProfessionalProfile profile;
    private VerificationRequest openRequest;

    @BeforeEach
    void setUp() {
        service = new UploadProfessionalDocumentService(professionalProfileRepositoryPort,
                verificationRequestRepositoryPort, verificationDocumentRepositoryPort,
                authenticatedUserPort, fileStoragePort, new StorageProperties());
        userId = UUID.randomUUID();
        profile = ProfessionalProfile.builder().id(UUID.randomUUID()).userId(userId).build();
        openRequest = VerificationRequest.builder().id(UUID.randomUUID()).status(VerificationRequestStatus.DRAFT).build();

        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "vet@example.com", true, false, Optional.empty()));
        lenient().when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(profile));
        lenient().when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId()))
                .thenReturn(Optional.of(openRequest));
        lenient().when(verificationDocumentRepositoryPort.findByRequestId(openRequest.getId())).thenReturn(List.of());
        lenient().when(verificationDocumentRepositoryPort.save(any(VerificationDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void uploadDocument_shouldSucceed_forProfessionalCard() {
        UploadProfessionalDocumentResponse response = service.uploadDocument("PROFESSIONAL_CARD", PDF_BYTES, "tarjeta.pdf");

        assertThat(response.getDocumentType()).isEqualTo("PROFESSIONAL_CARD");
        assertThat(response.getFileName()).isEqualTo("tarjeta.pdf");
        verify(fileStoragePort, times(1)).uploadPrivate(eq("verification-documents"), any(), any(), any());
    }

    @Test
    void uploadDocument_shouldThrowProfessionalProfileNotFound_whenCallerHasNoProfile() {
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.uploadDocument("PROFESSIONAL_CARD", PDF_BYTES, "tarjeta.pdf"))
                .isInstanceOf(ProfessionalProfileNotFoundException.class);
    }

    @Test
    void uploadDocument_shouldThrowVerificationLocked_whenRequestIsUnderReview() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId()))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.UNDER_REVIEW).build()));

        assertThatThrownBy(() -> service.uploadDocument("PROFESSIONAL_CARD", PDF_BYTES, "tarjeta.pdf"))
                .isInstanceOf(VerificationLockedException.class);
    }

    @Test
    void uploadDocument_shouldThrowInvalidFileType_whenContentIsNotARealDocument() {
        assertThatThrownBy(() -> service.uploadDocument("ID_DOCUMENT", NOT_A_FILE, "cedula.pdf"))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void uploadDocument_shouldThrowDocumentNotInCorrection_whenTypeWasNotRequested() {
        VerificationRequest needsCorrection = openRequest.toBuilder()
                .status(VerificationRequestStatus.NEEDS_CORRECTION)
                .correctionDocumentTypes(List.of("DEGREE"))
                .correctionRequestedAt(LocalDateTime.now())
                .build();
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId()))
                .thenReturn(Optional.of(needsCorrection));

        assertThatThrownBy(() -> service.uploadDocument("PROFESSIONAL_CARD", PDF_BYTES, "tarjeta.pdf"))
                .isInstanceOf(DocumentNotInCorrectionException.class);

        verify(fileStoragePort, never()).uploadPrivate(any(), any(), any(), any());
    }

    @Test
    void uploadDocument_shouldReplaceExistingDocumentOfSameType() {
        VerificationDocument existing = VerificationDocument.builder()
                .id(UUID.randomUUID())
                .requestId(openRequest.getId())
                .documentType("ID_DOCUMENT")
                .storagePath("verifications/" + openRequest.getId() + "/old.pdf")
                .build();
        when(verificationDocumentRepositoryPort.findByRequestId(openRequest.getId())).thenReturn(List.of(existing));

        service.uploadDocument("ID_DOCUMENT", PDF_BYTES, "cedula.pdf");

        verify(verificationDocumentRepositoryPort, times(1)).deleteById(existing.getId());
        verify(fileStoragePort, times(1)).delete("verification-documents", existing.getStoragePath());
    }
}
