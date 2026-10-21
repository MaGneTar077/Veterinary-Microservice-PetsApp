package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class ConsentRequiredException extends RuntimeException {

    public ConsentRequiredException() {
        super("acceptedDataPolicyVersion and acceptedTermsVersion are required");
    }

    public ConsentRequiredException(String message) {
        super(message);
    }
}
