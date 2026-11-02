package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class ClinicNotActiveException extends RuntimeException {
    public ClinicNotActiveException() {
        super("Only an ACTIVE clinic can be suspended");
    }
}
