package com.MyAnimaLog.Veterinary.application.professional.ports.in;

import java.util.UUID;

public interface DeleteProfessionalDocumentUseCase {
    void deleteDocument(UUID documentId);
}
