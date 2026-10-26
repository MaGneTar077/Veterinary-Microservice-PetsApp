package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RenewSignedUrlResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RenewVerificationDocumentSignedUrlUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationDocumentNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RenewVerificationDocumentSignedUrlService implements RenewVerificationDocumentSignedUrlUseCase {

    private static final Duration SIGNED_URL_TTL = Duration.ofMinutes(10);

    private final VeterinaryAuthorizationService authorizationService;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;

    @Override
    public RenewSignedUrlResponse renew(UUID requestId, UUID documentId) {
        authorizationService.requirePlatformAdmin();

        verificationRequestRepositoryPort.findById(requestId)
                .orElseThrow(VerificationRequestNotFoundException::new);

        VerificationDocument document = verificationDocumentRepositoryPort.findById(documentId)
                .filter(d -> d.getRequestId().equals(requestId))
                .orElseThrow(VerificationDocumentNotFoundException::new);

        String signedUrl = fileStoragePort.createSignedUrl(storageProperties.getVerificationBucket(), document.getStoragePath(), SIGNED_URL_TTL);

        return RenewSignedUrlResponse.builder()
                .signedUrl(signedUrl)
                .expiresAt(LocalDateTime.now().plus(SIGNED_URL_TTL))
                .build();
    }
}
