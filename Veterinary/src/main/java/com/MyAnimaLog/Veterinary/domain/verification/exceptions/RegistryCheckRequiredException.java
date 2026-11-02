package com.MyAnimaLog.Veterinary.domain.verification.exceptions;

public class RegistryCheckRequiredException extends RuntimeException {

    public RegistryCheckRequiredException() {
        super("registryChecked must be true to approve a verification request");
    }

    public RegistryCheckRequiredException(String message) {
        super(message);
    }
}
