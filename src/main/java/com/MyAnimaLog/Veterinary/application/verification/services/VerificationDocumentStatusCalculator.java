package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;

/**
 * The per-document status shown to the clinic/professional in their own GET endpoints
 * (PENDING/READY/NEEDS_CORRECTION/REVIEWED — FASE_2B_VERIFICACION.md §3), shared between
 * {@code GetClinicVerificationService} and {@code GetMyProfessionalProfileService}.
 */
public final class VerificationDocumentStatusCalculator {

    private VerificationDocumentStatusCalculator() {
    }

    public static String statusFor(VerificationRequest request, String documentType, VerificationDocument document) {
        if (document == null) {
            return "PENDING";
        }
        boolean requestedForCorrection = request.getStatus() == VerificationRequestStatus.NEEDS_CORRECTION
                && request.getCorrectionDocumentTypes() != null
                && request.getCorrectionDocumentTypes().contains(documentType);
        if (requestedForCorrection) {
            boolean fixedAfterRequest = document.getUploadedAt() != null
                    && request.getCorrectionRequestedAt() != null
                    && !document.getUploadedAt().isBefore(request.getCorrectionRequestedAt());
            return fixedAfterRequest ? "READY" : "NEEDS_CORRECTION";
        }
        if (request.getStatus() == VerificationRequestStatus.NEEDS_CORRECTION) {
            return "REVIEWED";
        }
        return "READY";
    }
}
