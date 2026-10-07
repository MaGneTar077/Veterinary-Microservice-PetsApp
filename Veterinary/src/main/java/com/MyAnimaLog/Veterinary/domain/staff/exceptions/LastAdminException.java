package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

public class LastAdminException extends RuntimeException {

    public LastAdminException() {
        super("Cannot deactivate or demote the last active admin of this veterinary");
    }

    public LastAdminException(String message) {
        super(message);
    }
}
