package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class InvalidVeterinaryFieldException extends RuntimeException {

    public InvalidVeterinaryFieldException(String message) {
        super(message);
    }
}
