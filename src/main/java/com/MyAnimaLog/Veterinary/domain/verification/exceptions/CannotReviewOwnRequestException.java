package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class CannotReviewOwnRequestException extends RuntimeException {

    public CannotReviewOwnRequestException() {
        super("An admin cannot review a verification request they submitted themselves");
    }

    public CannotReviewOwnRequestException(String message) {
        super(message);
    }
}
