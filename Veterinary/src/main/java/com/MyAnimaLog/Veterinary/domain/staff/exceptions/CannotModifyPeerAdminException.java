package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

public class CannotModifyPeerAdminException extends RuntimeException {

    public CannotModifyPeerAdminException() {
        super("An ADMIN cannot modify another ADMIN");
    }

    public CannotModifyPeerAdminException(String message) {
        super(message);
    }
}
