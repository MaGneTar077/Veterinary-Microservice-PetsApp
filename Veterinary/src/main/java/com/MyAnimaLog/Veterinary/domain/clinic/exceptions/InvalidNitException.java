package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class InvalidNitException extends RuntimeException {

    public InvalidNitException() {
        super("NIT is required and must match the format 123456789-0 with a valid check digit");
    }

    public InvalidNitException(String message) {
        super(message);
    }
}
