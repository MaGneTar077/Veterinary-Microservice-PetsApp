package com.MyAnimaLog.Veterinary.domain.professional.exceptions;

public class ProfessionalProfileNotFoundException extends RuntimeException {

    public ProfessionalProfileNotFoundException() {
        super("Professional profile not found");
    }

    public ProfessionalProfileNotFoundException(String message) {
        super(message);
    }
}
