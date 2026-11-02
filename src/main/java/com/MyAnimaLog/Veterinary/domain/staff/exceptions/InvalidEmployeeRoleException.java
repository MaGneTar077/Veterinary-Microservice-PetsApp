package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

public class InvalidEmployeeRoleException extends RuntimeException {

    public InvalidEmployeeRoleException() {
        super("Employee role is invalid");
    }

    public InvalidEmployeeRoleException(String message) {
        super(message);
    }
}
