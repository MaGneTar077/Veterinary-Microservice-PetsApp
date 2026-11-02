package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.domain.shared.exceptions.DocumentNotInCorrectionException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;

/**
 * While a request is {@code NEEDS_CORRECTION}, only the document types the admin actually asked
 * for can be uploaded or deleted — everything else was already reviewed and accepted. Shared by
 * both the clinic and the professional-profile document services (item 4, Fase 2B Parte 4A).
 */
public final class CorrectionGate {

    private CorrectionGate() {
    }

    public static void checkDocumentTypeAllowed(VerificationRequest request, String documentType) {
        if (request.getStatus() != VerificationRequestStatus.NEEDS_CORRECTION) {
            return;
        }
        if (request.getCorrectionDocumentTypes() == null || !request.getCorrectionDocumentTypes().contains(documentType)) {
            throw new DocumentNotInCorrectionException(
                    "Document type " + documentType + " was not requested for correction");
        }
    }
}
