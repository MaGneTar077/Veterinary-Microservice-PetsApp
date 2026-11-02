package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class TooManyOwnedVeterinariesException extends RuntimeException {

    public TooManyOwnedVeterinariesException() {
        super("A user can own at most 3 veterinaries that are not in a final state (REJECTED)");
    }

    public TooManyOwnedVeterinariesException(String message) {
        super(message);
    }
}
