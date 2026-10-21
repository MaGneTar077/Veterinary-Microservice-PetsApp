package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.UploadVerificationDocumentResponse;

import java.util.UUID;

public interface UploadVerificationDocumentUseCase {
    UploadVerificationDocumentResponse uploadDocument(UUID veterinaryId, String documentType, byte[] content);
}
