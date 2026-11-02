package com.MyAnimaLog.Veterinary.domain.patients.exceptions;

public class UserAlreadyLinkedException extends RuntimeException {

    public UserAlreadyLinkedException() {
        super("User is already linked to this veterinary");
    }

    public UserAlreadyLinkedException(String message) {
        super(message);
    }
}
