package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class VerificationLockedException extends RuntimeException {

    public VerificationLockedException() {
        super("This verification request is not editable in its current state");
    }

    public VerificationLockedException(String message) {
        super(message);
    }
}
