package com.MyAnimaLog.Veterinary.domain.exceptions;

public class EmployeeAlreadyExistsException extends RuntimeException {

    public EmployeeAlreadyExistsException() {
        super("Employee already exists in this veterinary");
    }

    public EmployeeAlreadyExistsException(String message) {
        super(message);
    }
}
