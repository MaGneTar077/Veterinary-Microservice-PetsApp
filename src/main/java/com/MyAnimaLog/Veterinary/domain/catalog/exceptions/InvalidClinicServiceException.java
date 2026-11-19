package com.MyAnimaLog.Veterinary.domain.catalog.exceptions;

public class InvalidClinicServiceException extends RuntimeException {

    public InvalidClinicServiceException(String message) {
        super(message);
    }
}
