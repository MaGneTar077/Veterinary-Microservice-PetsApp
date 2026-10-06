package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class ClinicContextRequiredException extends RuntimeException {

    public ClinicContextRequiredException() {
        super("This operation requires a clinic (veterinary) token context");
    }

    public ClinicContextRequiredException(String message) {
        super(message);
    }
}
