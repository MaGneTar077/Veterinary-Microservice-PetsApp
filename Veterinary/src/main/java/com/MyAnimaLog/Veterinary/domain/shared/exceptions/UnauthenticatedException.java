package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
        super("No authenticated user in the current request");
    }

    public UnauthenticatedException(String message) {
        super(message);
    }
}
