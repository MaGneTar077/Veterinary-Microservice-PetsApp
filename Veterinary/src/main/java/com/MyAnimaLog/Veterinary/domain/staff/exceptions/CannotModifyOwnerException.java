package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

public class CannotModifyOwnerException extends RuntimeException {

    public CannotModifyOwnerException() {
        super("The clinic owner cannot be deactivated or have their role changed");
    }

    public CannotModifyOwnerException(String message) {
        super(message);
    }
}
