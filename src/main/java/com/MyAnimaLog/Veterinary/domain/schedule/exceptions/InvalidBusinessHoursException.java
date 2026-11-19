package com.MyAnimaLog.Veterinary.domain.schedule.exceptions;

public class InvalidBusinessHoursException extends RuntimeException {

    public InvalidBusinessHoursException(String message) {
        super(message);
    }
}
