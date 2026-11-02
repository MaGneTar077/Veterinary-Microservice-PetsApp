package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class VerificationAttemptsExhaustedException extends RuntimeException {

    public VerificationAttemptsExhaustedException() {
        super("The maximum number of verification attempts has been reached; contact support");
    }

    public VerificationAttemptsExhaustedException(String message) {
        super(message);
    }
}
