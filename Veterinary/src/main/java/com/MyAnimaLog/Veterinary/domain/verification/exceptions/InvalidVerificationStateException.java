package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class InvalidVerificationStateException extends RuntimeException {

    public InvalidVerificationStateException() {
        super("The verification request is not in a state that allows this action");
    }

    public InvalidVerificationStateException(String message) {
        super(message);
    }
}
