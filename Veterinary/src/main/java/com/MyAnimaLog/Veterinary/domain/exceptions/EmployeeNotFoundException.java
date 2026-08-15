package com.MyAnimaLog.Veterinary.domain.exceptions;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException() {
        super("Employee not found");
    }

    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
