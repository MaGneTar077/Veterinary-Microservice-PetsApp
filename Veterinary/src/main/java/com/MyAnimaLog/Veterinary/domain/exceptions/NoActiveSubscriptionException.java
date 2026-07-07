package com.MyAnimaLog.Veterinary.domain.exceptions;

public class NoActiveSubscriptionException extends RuntimeException {

    public NoActiveSubscriptionException() {
        super("Veterinary has no active subscription");
    }

    public NoActiveSubscriptionException(String message) {
        super(message);
    }
}
