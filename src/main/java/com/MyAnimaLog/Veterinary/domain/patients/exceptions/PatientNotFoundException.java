package com.MyAnimaLog.Veterinary.domain.patients.exceptions;

/** La mascota no está compartida activamente con esa clínica (nunca se compartió, o se revocó). */
public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException() {
        super("This pet is not an active patient of this veterinary");
    }
}
