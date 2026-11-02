package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class NotVeterinaryOwnerException extends RuntimeException {

    public NotVeterinaryOwnerException() {
        super("Only the OWNER of this veterinary can perform this action");
    }

    public NotVeterinaryOwnerException(String message) {
        super(message);
    }
}
