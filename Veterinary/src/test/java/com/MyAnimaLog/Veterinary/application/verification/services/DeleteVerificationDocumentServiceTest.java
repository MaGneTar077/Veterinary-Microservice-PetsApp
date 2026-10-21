package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationDocumentNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteVerificationDocumentServiceTest {

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private FileStoragePort fileStoragePort;

    private DeleteVerificationDocumentService service;
    private UUID veterinaryId;
    private VerificationRequest openRequest;
    private VerificationDocument document;

    @BeforeEach
    void setUp() {
        service = new DeleteVerificationDocumentService(verificationRequestRepositoryPort,
                verificationDocumentRepositoryPort, authorizationService, fileStoragePort, new StorageProperties());
        veterinaryId = UUID.randomUUID();
        openRequest = VerificationRequest.builder().id(UUID.randomUUID()).status(VerificationRequestStatus.DRAFT).build();
        document = VerificationDocument.builder()
                .id(UUID.randomUUID())
                .requestId(openRequest.getId())
                .storagePath("verifications/" + openRequest.getId() + "/doc.pdf")
                .build();

        lenient().when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(openRequest));
    }

    @Test
    void deleteDocument_shouldDeleteRowAndFile_whenDocumentBelongsToOpenRequest() {
        when(verificationDocumentRepositoryPort.findById(document.getId())).thenReturn(Optional.of(document));

        service.deleteDocument(veterinaryId, document.getId());

        verify(verificationDocumentRepositoryPort, times(1)).deleteById(document.getId());
        verify(fileStoragePort, times(1)).delete("verification-documents", document.getStoragePath());
    }

    @Test
    void deleteDocument_shouldThrowNotFound_whenDocumentBelongsToAnotherRequest() {
        VerificationDocument otherRequestDoc = document.toBuilder().requestId(UUID.randomUUID()).build();
        when(verificationDocumentRepositoryPort.findById(document.getId())).thenReturn(Optional.of(otherRequestDoc));

        assertThatThrownBy(() -> service.deleteDocument(veterinaryId, document.getId()))
                .isInstanceOf(VerificationDocumentNotFoundException.class);
    }

    @Test
    void deleteDocument_shouldThrowNotFound_whenDocumentDoesNotExist() {
        when(verificationDocumentRepositoryPort.findById(document.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDocument(veterinaryId, document.getId()))
                .isInstanceOf(VerificationDocumentNotFoundException.class);
    }

    @Test
    void deleteDocument_shouldThrowVerificationLocked_whenRequestIsUnderReview() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.UNDER_REVIEW).build()));

        assertThatThrownBy(() -> service.deleteDocument(veterinaryId, document.getId()))
                .isInstanceOf(VerificationLockedException.class);
    }

    @Test
    void deleteDocument_shouldPropagateNotVeterinaryOwner() {
        doThrow(new NotVeterinaryOwnerException()).when(authorizationService).requireVeterinaryOwner(veterinaryId);

        assertThatThrownBy(() -> service.deleteDocument(veterinaryId, document.getId()))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }
}
