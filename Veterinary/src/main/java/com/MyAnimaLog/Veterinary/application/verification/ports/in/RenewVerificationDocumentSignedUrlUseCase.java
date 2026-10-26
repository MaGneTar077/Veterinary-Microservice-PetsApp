package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.RenewSignedUrlResponse;

import java.util.UUID;

public interface RenewVerificationDocumentSignedUrlUseCase {
    RenewSignedUrlResponse renew(UUID requestId, UUID documentId);
}
