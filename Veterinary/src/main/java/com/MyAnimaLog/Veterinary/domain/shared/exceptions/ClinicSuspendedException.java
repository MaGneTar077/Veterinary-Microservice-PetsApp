package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class ClinicSuspendedException extends RuntimeException {
    public ClinicSuspendedException() {
        super("This clinic is suspended");
    }
}
