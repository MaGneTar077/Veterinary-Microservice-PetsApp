package com.MyAnimaLog.Veterinary.domain.exceptions;

public class UserNotLinkedException extends RuntimeException {

    public UserNotLinkedException() {
        super("User is not linked to this veterinary");
    }

    public UserNotLinkedException(String message) {
        super(message);
    }
}
