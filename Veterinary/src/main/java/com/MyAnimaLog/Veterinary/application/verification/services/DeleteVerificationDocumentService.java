package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.DeleteVerificationDocumentUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationDocumentNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteVerificationDocumentService implements DeleteVerificationDocumentUseCase {

    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;

    @Override
    public void deleteDocument(UUID veterinaryId, UUID documentId) {
        authorizationService.requireVeterinaryOwner(veterinaryId);

        VerificationRequest request = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId)
                .filter(r -> r.getStatus() == VerificationRequestStatus.DRAFT || r.getStatus() == VerificationRequestStatus.NEEDS_CORRECTION)
                .orElseThrow(VerificationLockedException::new);

        VerificationDocument document = verificationDocumentRepositoryPort.findById(documentId)
                .filter(d -> d.getRequestId().equals(request.getId()))
                .orElseThrow(VerificationDocumentNotFoundException::new);

        verificationDocumentRepositoryPort.deleteById(document.getId());
        fileStoragePort.delete(storageProperties.getVerificationBucket(), document.getStoragePath());
    }
}
