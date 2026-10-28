package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class InvalidSuspensionReasonException extends RuntimeException {
    public InvalidSuspensionReasonException(String message) {
        super(message);
    }
}
