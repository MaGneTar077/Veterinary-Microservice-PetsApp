package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class TruthfulnessDeclarationRequiredException extends RuntimeException {

    public TruthfulnessDeclarationRequiredException() {
        super("truthfulnessDeclared must be true to submit a verification request");
    }

    public TruthfulnessDeclarationRequiredException(String message) {
        super(message);
    }
}
