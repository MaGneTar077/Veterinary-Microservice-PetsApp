package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class ExternalServiceUnavailableException extends RuntimeException {

    public ExternalServiceUnavailableException() {
        super("An external service is currently unavailable");
    }

    public ExternalServiceUnavailableException(String message) {
        super(message);
    }

    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
