package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class VerificationDocumentNotFoundException extends RuntimeException {

    public VerificationDocumentNotFoundException() {
        super("Verification document not found");
    }

    public VerificationDocumentNotFoundException(String message) {
        super(message);
    }
}
