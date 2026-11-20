package com.MyAnimaLog.Veterinary.domain.schedule.exceptions;

public class InvalidEmployeeScheduleException extends RuntimeException {

    public InvalidEmployeeScheduleException(String message) {
        super(message);
    }
}
