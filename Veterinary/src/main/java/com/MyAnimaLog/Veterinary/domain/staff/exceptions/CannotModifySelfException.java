package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

public class CannotModifySelfException extends RuntimeException {

    public CannotModifySelfException() {
        super("You cannot change your own role or activation status");
    }

    public CannotModifySelfException(String message) {
        super(message);
    }
}
