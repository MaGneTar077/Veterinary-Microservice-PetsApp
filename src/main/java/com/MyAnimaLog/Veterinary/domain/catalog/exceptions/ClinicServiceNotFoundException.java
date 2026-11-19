package com.MyAnimaLog.Veterinary.domain.catalog.exceptions;

/** No existe, o existe en otra clínica — mismo error en los dos casos: no se delata el segundo caso. */
public class ClinicServiceNotFoundException extends RuntimeException {

    public ClinicServiceNotFoundException() {
        super("Service not found for this veterinary");
    }
}
