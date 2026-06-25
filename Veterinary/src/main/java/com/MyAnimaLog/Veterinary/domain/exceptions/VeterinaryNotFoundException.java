package com.MyAnimaLog.Veterinary.domain.exceptions;

public class VeterinaryNotFoundException extends RuntimeException {

    public VeterinaryNotFoundException() {
        super("Veterinary not found");
    }

    public VeterinaryNotFoundException(String message) {
        super(message);
    }
}
