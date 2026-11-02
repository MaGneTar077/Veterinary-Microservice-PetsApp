package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.verification.dto.CleanupVerificationDocumentsResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.CleanupVerificationDocumentsJobUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Retention job (D11, FASE_2B_VERIFICACION.md §6): deletes documents (storage + row) belonging to
 * {@code REJECTED} requests decided more than 90 days ago. Idempotent — a second run simply finds
 * nothing left to delete for requests already cleaned up. Cloud Scheduler wiring is out of scope
 * here (Fase 6); this only exposes the endpoint it will call.
 */
@Service
@RequiredArgsConstructor
public class CleanupVerificationDocumentsJobService implements CleanupVerificationDocumentsJobUseCase {

    private static final int RETENTION_DAYS = 90;

    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;

    @Override
    @Transactional
    public CleanupVerificationDocumentsResponse cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<VerificationRequest> rejectedRequests = verificationRequestRepositoryPort.findRejectedDecidedBefore(cutoff);

        int deletedDocuments = 0;
        for (VerificationRequest request : rejectedRequests) {
            for (VerificationDocument document : verificationDocumentRepositoryPort.findByRequestId(request.getId())) {
                fileStoragePort.delete(storageProperties.getVerificationBucket(), document.getStoragePath());
                verificationDocumentRepositoryPort.deleteById(document.getId());
                deletedDocuments++;
            }
        }

        return CleanupVerificationDocumentsResponse.builder()
                .affectedRequests(rejectedRequests.size())
                .deletedDocuments(deletedDocuments)
                .build();
    }
}
