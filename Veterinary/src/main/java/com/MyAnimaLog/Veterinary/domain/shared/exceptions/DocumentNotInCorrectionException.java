package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class DocumentNotInCorrectionException extends RuntimeException {

    public DocumentNotInCorrectionException() {
        super("This document type was not requested for correction");
    }

    public DocumentNotInCorrectionException(String message) {
        super(message);
    }
}
