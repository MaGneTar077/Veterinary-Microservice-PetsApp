package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class TenantMismatchException extends RuntimeException {

    public TenantMismatchException() {
        super("The veterinaryId in the request does not match the token's clinic context");
    }

    public TenantMismatchException(String message) {
        super(message);
    }
}
