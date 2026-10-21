package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException() {
        super("The file type is not allowed");
    }

    public InvalidFileTypeException(String message) {
        super(message);
    }
}
