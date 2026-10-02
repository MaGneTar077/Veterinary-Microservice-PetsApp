package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class InvalidVeterinaryNameException extends RuntimeException {

    public InvalidVeterinaryNameException() {
        super("Veterinary name is invalid");
    }

    public InvalidVeterinaryNameException(String message) {
        super(message);
    }
}
