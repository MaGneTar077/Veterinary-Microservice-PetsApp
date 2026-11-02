package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.verification.dto.CleanupVerificationDocumentsResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupVerificationDocumentsJobServiceTest {

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    @Mock
    private FileStoragePort fileStoragePort;

    private final StorageProperties storageProperties = new StorageProperties();

    private CleanupVerificationDocumentsJobService service;

    @Test
    void cleanup_shouldDeleteDocumentsAndFiles_forRejectedRequestsPastRetention() {
        service = new CleanupVerificationDocumentsJobService(verificationRequestRepositoryPort,
                verificationDocumentRepositoryPort, fileStoragePort, storageProperties);

        VerificationRequest rejected = VerificationRequest.builder()
                .id(UUID.randomUUID()).subjectType(SubjectType.CLINIC).subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.REJECTED).attemptNumber(3)
                .decidedAt(LocalDateTime.now().minusDays(91)).build();
        when(verificationRequestRepositoryPort.findRejectedDecidedBefore(ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(rejected));

        VerificationDocument doc1 = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(rejected.getId()).documentType("RUT")
                .storagePath("verifications/" + rejected.getId() + "/a.pdf").build();
        VerificationDocument doc2 = VerificationDocument.builder()
                .id(UUID.randomUUID()).requestId(rejected.getId()).documentType("CHAMBER_OF_COMMERCE")
                .storagePath("verifications/" + rejected.getId() + "/b.pdf").build();
        when(verificationDocumentRepositoryPort.findByRequestId(rejected.getId())).thenReturn(List.of(doc1, doc2));

        CleanupVerificationDocumentsResponse response = service.cleanup();

        assertThat(response.getAffectedRequests()).isEqualTo(1);
        assertThat(response.getDeletedDocuments()).isEqualTo(2);
        verify(fileStoragePort).delete(storageProperties.getVerificationBucket(), doc1.getStoragePath());
        verify(fileStoragePort).delete(storageProperties.getVerificationBucket(), doc2.getStoragePath());
        verify(verificationDocumentRepositoryPort).deleteById(doc1.getId());
        verify(verificationDocumentRepositoryPort).deleteById(doc2.getId());
    }

    @Test
    void cleanup_shouldBeIdempotent_whenNothingLeftToDelete() {
        service = new CleanupVerificationDocumentsJobService(verificationRequestRepositoryPort,
                verificationDocumentRepositoryPort, fileStoragePort, storageProperties);

        when(verificationRequestRepositoryPort.findRejectedDecidedBefore(any(LocalDateTime.class))).thenReturn(List.of());

        CleanupVerificationDocumentsResponse response = service.cleanup();

        assertThat(response.getAffectedRequests()).isZero();
        assertThat(response.getDeletedDocuments()).isZero();
        verify(fileStoragePort, never()).delete(any(), any());
        verify(verificationDocumentRepositoryPort, never()).deleteById(any());
    }
}
