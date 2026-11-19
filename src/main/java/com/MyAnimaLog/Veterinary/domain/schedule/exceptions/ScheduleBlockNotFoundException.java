package com.MyAnimaLog.Veterinary.domain.schedule.exceptions;

/** No existe, o es de otra clínica — mismo error en los dos casos: no se delata el segundo. */
public class ScheduleBlockNotFoundException extends RuntimeException {

    public ScheduleBlockNotFoundException() {
        super("Schedule block not found for this veterinary");
    }
}
