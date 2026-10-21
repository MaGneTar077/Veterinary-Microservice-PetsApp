package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import java.util.UUID;

public interface DeleteVerificationDocumentUseCase {
    void deleteDocument(UUID veterinaryId, UUID documentId);
}
