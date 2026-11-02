package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class ClinicNotSuspendedException extends RuntimeException {
    public ClinicNotSuspendedException() {
        super("Only a SUSPENDED clinic can be reactivated");
    }
}
