package com.MyAnimaLog.Veterinary.domain.exceptions;

public class VeterinaryNotActiveException extends RuntimeException {

    public VeterinaryNotActiveException() {
        super("Veterinary is not active");
    }

    public VeterinaryNotActiveException(String message) {
        super(message);
    }
}
