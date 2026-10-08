package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class InvalidVeterinaryStatusTransitionException extends RuntimeException {

    public InvalidVeterinaryStatusTransitionException() {
        super("Veterinary is not in a status that allows this transition");
    }

    public InvalidVeterinaryStatusTransitionException(String message) {
        super(message);
    }
}
