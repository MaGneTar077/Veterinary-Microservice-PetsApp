package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class AiVerificationDisabledException extends RuntimeException {

    public AiVerificationDisabledException() {
        super("AI verification is disabled (AI_VERIFICATION_ENABLED=false)");
    }

    public AiVerificationDisabledException(String message) {
        super(message);
    }
}
