package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class InvalidDocumentTypeException extends RuntimeException {

    public InvalidDocumentTypeException(String message) {
        super(message);
    }
}
