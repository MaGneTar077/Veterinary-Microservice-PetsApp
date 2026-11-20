package com.MyAnimaLog.Veterinary.domain.schedule.exceptions;

public class InvalidScheduleRangeException extends RuntimeException {

    public InvalidScheduleRangeException(String message) {
        super(message);
    }
}
