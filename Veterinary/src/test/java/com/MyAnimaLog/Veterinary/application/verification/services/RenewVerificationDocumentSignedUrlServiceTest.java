package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RenewSignedUrlResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationDocumentNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
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
class RenewVerificationDocumentSignedUrlServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    @Mock
    private FileStoragePort fileStoragePort;

    private StorageProperties storageProperties;

    private RenewVerificationDocumentSignedUrlService service;

    private UUID requestId;
    private UUID documentId;
    private VerificationDocument document;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        service = new RenewVerificationDocumentSignedUrlService(authorizationService, verificationRequestRepositoryPort,
                verificationDocumentRepositoryPort, fileStoragePort, storageProperties);

        requestId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        document = VerificationDocument.builder()
                .id(documentId).requestId(requestId).documentType("RUT")
                .storagePath("verifications/" + requestId + "/doc.pdf").build();

        lenient().when(verificationRequestRepositoryPort.findById(requestId))
                .thenReturn(Optional.of(VerificationRequest.builder().id(requestId).build()));
        lenient().when(verificationDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
    }

    @Test
    void renew_shouldReturnNewSignedUrl() {
        when(fileStoragePort.createSignedUrl(eq(storageProperties.getVerificationBucket()), eq(document.getStoragePath()), any(Duration.class)))
                .thenReturn("https://signed.example/doc.pdf");

        RenewSignedUrlResponse response = service.renew(requestId, documentId);

        assertThat(response.getSignedUrl()).isEqualTo("https://signed.example/doc.pdf");
        assertThat(response.getExpiresAt()).isNotNull();
    }

    @Test
    void renew_shouldThrowVerificationRequestNotFound_whenRequestMissing() {
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renew(requestId, documentId))
                .isInstanceOf(VerificationRequestNotFoundException.class);
    }

    @Test
    void renew_shouldThrowDocumentNotFound_whenDocumentBelongsToAnotherRequest() {
        VerificationDocument other = document.toBuilder().requestId(UUID.randomUUID()).build();
        when(verificationDocumentRepositoryPort.findById(documentId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.renew(requestId, documentId))
                .isInstanceOf(VerificationDocumentNotFoundException.class);
    }

    @Test
    void renew_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.renew(requestId, documentId))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }
}
