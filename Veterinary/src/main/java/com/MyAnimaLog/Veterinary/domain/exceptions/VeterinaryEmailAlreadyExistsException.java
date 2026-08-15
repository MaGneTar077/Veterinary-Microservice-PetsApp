package com.MyAnimaLog.Veterinary.domain.exceptions;

public class VeterinaryEmailAlreadyExistsException extends RuntimeException {

    public VeterinaryEmailAlreadyExistsException() {
        super("A veterinary with this email already exists");
    }

    public VeterinaryEmailAlreadyExistsException(String message) {
        super(message);
    }
}
