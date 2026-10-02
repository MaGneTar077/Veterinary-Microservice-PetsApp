package com.MyAnimaLog.Veterinary.domain.patients.exceptions;

public class InvalidInviteCodeException extends RuntimeException {

    public InvalidInviteCodeException() {
        super("Invite code is invalid or does not exist");
    }

    public InvalidInviteCodeException(String message) {
        super(message);
    }
}
