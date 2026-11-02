package com.MyAnimaLog.Veterinary.application.professional.ports.in;

import com.MyAnimaLog.Veterinary.application.professional.dto.UploadProfessionalDocumentResponse;

public interface UploadProfessionalDocumentUseCase {
    UploadProfessionalDocumentResponse uploadDocument(String documentType, byte[] content, String originalFileName);
}
