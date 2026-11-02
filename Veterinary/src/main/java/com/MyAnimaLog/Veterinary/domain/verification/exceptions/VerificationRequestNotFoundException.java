package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class VerificationRequestNotFoundException extends RuntimeException {

    public VerificationRequestNotFoundException() {
        super("Verification request not found");
    }

    public VerificationRequestNotFoundException(String message) {
        super(message);
    }
}
