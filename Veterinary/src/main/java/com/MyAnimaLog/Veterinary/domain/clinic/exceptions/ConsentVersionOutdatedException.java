package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class ConsentVersionOutdatedException extends RuntimeException {

    public ConsentVersionOutdatedException() {
        super("The accepted consent version is outdated");
    }

    public ConsentVersionOutdatedException(String message) {
        super(message);
    }
}
