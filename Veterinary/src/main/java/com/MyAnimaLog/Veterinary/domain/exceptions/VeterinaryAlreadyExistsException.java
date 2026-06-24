package com.MyAnimaLog.Veterinary.domain.exceptions;

public class VeterinaryAlreadyExistsException extends RuntimeException {

    public VeterinaryAlreadyExistsException() {
        super("Veterinary already exists");
    }

    public VeterinaryAlreadyExistsException(String message) {
        super(message);
    }
}
