package com.MyAnimaLog.Veterinary.domain.exceptions;

public class InvalidVeterinaryEmailException extends RuntimeException {

    public InvalidVeterinaryEmailException() {
        super("Veterinary email is invalid");
    }

    public InvalidVeterinaryEmailException(String message) {
        super(message);
    }
}
