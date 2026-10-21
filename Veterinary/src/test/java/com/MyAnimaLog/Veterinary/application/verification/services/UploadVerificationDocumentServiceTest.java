package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.UploadVerificationDocumentResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.FileTooLargeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidDocumentTypeException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadVerificationDocumentServiceTest {

    private static final byte[] PDF_BYTES = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};
    private static final byte[] NOT_A_FILE = {1, 2, 3, 4};

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private FileStoragePort fileStoragePort;

    private UploadVerificationDocumentService service;
    private UUID veterinaryId;
    private VerificationRequest openRequest;

    @BeforeEach
    void setUp() {
        service = new UploadVerificationDocumentService(verificationRequestRepositoryPort,
                verificationDocumentRepositoryPort, authorizationService, fileStoragePort, new StorageProperties());
        veterinaryId = UUID.randomUUID();
        openRequest = VerificationRequest.builder().id(UUID.randomUUID()).status(VerificationRequestStatus.DRAFT).build();

        lenient().when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(openRequest));
        lenient().when(verificationDocumentRepositoryPort.findByRequestId(openRequest.getId()))
                .thenReturn(List.of());
        lenient().when(verificationDocumentRepositoryPort.save(any(VerificationDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void uploadDocument_shouldSucceed_forPdfRut() {
        UploadVerificationDocumentResponse response = service.uploadDocument(veterinaryId, "RUT", PDF_BYTES);

        assertThat(response.getDocumentType()).isEqualTo("RUT");
        assertThat(response.getFileName()).endsWith(".pdf");
        verify(fileStoragePort, times(1)).uploadPrivate(eq("verification-documents"), any(), any(), any());
    }

    @Test
    void uploadDocument_shouldThrowInvalidFileType_whenContentIsNotARealDocument() {
        assertThatThrownBy(() -> service.uploadDocument(veterinaryId, "RUT", NOT_A_FILE))
                .isInstanceOf(InvalidFileTypeException.class);

        verify(fileStoragePort, never()).uploadPrivate(any(), any(), any(), any());
    }

    @Test
    void uploadDocument_shouldThrowFileTooLarge_whenContentExceeds10Mb() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PDF_BYTES, 0, big, 0, PDF_BYTES.length);

        assertThatThrownBy(() -> service.uploadDocument(veterinaryId, "RUT", big))
                .isInstanceOf(FileTooLargeException.class);
    }

    @Test
    void uploadDocument_shouldThrowInvalidDocumentType_whenTypeIsUnknown() {
        assertThatThrownBy(() -> service.uploadDocument(veterinaryId, "PASSPORT", PDF_BYTES))
                .isInstanceOf(InvalidDocumentTypeException.class);
    }

    @Test
    void uploadDocument_shouldThrowVerificationLocked_whenRequestIsUnderReview() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.UNDER_REVIEW).build()));

        assertThatThrownBy(() -> service.uploadDocument(veterinaryId, "RUT", PDF_BYTES))
                .isInstanceOf(VerificationLockedException.class);
    }

    @Test
    void uploadDocument_shouldPropagateNotVeterinaryOwner() {
        doThrow(new NotVeterinaryOwnerException()).when(authorizationService).requireVeterinaryOwner(veterinaryId);

        assertThatThrownBy(() -> service.uploadDocument(veterinaryId, "RUT", PDF_BYTES))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    @Test
    void uploadDocument_shouldReplaceExistingDocumentOfSameType_deletingOldRowAndOldFile() {
        VerificationDocument existing = VerificationDocument.builder()
                .id(UUID.randomUUID())
                .requestId(openRequest.getId())
                .documentType("RUT")
                .storagePath("verifications/" + openRequest.getId() + "/old-id.pdf")
                .build();
        when(verificationDocumentRepositoryPort.findByRequestId(openRequest.getId())).thenReturn(List.of(existing));

        service.uploadDocument(veterinaryId, "RUT", PDF_BYTES);

        verify(verificationDocumentRepositoryPort, times(1)).deleteById(existing.getId());
        verify(fileStoragePort, times(1)).delete("verification-documents", existing.getStoragePath());
    }

    @Test
    void uploadDocument_shouldCompensateByDeletingUploadedFile_whenSaveFails() {
        when(verificationDocumentRepositoryPort.save(any(VerificationDocument.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.uploadDocument(veterinaryId, "RUT", PDF_BYTES))
                .isInstanceOf(RuntimeException.class);

        verify(fileStoragePort, times(1)).delete(eq("verification-documents"), any());
    }
}
