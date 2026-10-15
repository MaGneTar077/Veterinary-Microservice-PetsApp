package com.MyAnimaLog.Veterinary.application.verification.ports.out;

import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationDocumentRepositoryPort {
    Optional<VerificationDocument> findById(UUID id);
    List<VerificationDocument> findByRequestId(UUID requestId);
    VerificationDocument save(VerificationDocument document);
    void deleteById(UUID id);
}
