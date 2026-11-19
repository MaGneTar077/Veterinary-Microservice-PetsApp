package com.MyAnimaLog.Veterinary.domain.catalog.exceptions;

/** Ya hay un servicio activo con ese nombre (sin distinguir mayúsculas) en la misma clínica. */
public class ClinicServiceNameAlreadyExistsException extends RuntimeException {

    public ClinicServiceNameAlreadyExistsException() {
        super("A service with this name already exists for this veterinary");
    }
}
