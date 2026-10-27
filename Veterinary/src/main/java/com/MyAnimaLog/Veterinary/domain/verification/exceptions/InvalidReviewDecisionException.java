package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class InvalidReviewDecisionException extends RuntimeException {

    public InvalidReviewDecisionException() {
        super("Invalid review decision");
    }

    public InvalidReviewDecisionException(String message) {
        super(message);
    }
}
