package com.MyAnimaLog.Veterinary.domain.exceptions;

public class ActiveSubscriptionAlreadyExistsException extends RuntimeException {

    public ActiveSubscriptionAlreadyExistsException() {
        super("Veterinary already has an active subscription");
    }

    public ActiveSubscriptionAlreadyExistsException(String message) {
        super(message);
    }
}
