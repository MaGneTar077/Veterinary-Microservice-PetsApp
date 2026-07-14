package com.MyAnimaLog.Veterinary.domain.exceptions;

public class InvalidInviteCodeException extends RuntimeException {

    public InvalidInviteCodeException() {
        super("Invite code is invalid or does not exist");
    }

    public InvalidInviteCodeException(String message) {
        super(message);
    }
}
