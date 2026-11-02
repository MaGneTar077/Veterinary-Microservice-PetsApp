package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException() {
        super("The file exceeds the maximum allowed size");
    }

    public FileTooLargeException(String message) {
        super(message);
    }
}
