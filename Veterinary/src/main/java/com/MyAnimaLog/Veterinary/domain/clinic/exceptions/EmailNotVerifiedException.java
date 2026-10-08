package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("This operation requires a verified email (email_verified=true in the token)");
    }

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
