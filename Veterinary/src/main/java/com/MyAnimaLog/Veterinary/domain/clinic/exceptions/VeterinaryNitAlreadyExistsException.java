package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class VeterinaryNitAlreadyExistsException extends RuntimeException {

    public VeterinaryNitAlreadyExistsException() {
        super("A veterinary with this NIT already exists");
    }

    public VeterinaryNitAlreadyExistsException(String message) {
        super(message);
    }
}
