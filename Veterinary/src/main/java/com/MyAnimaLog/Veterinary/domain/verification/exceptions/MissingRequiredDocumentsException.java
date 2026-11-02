package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class MissingRequiredDocumentsException extends RuntimeException {

    public MissingRequiredDocumentsException(String message) {
        super(message);
    }
}
