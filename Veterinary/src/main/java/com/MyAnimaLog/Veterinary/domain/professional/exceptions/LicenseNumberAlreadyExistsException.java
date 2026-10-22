package com.MyAnimaLog.Veterinary.domain.professional.exceptions;

public class LicenseNumberAlreadyExistsException extends RuntimeException {

    public LicenseNumberAlreadyExistsException() {
        super("A professional profile with this license number already exists");
    }

    public LicenseNumberAlreadyExistsException(String message) {
        super(message);
    }
}
