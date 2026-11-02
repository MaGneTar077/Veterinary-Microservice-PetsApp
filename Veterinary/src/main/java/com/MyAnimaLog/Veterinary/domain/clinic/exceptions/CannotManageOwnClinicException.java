package com.MyAnimaLog.Veterinary.domain.clinic.exceptions;

public class CannotManageOwnClinicException extends RuntimeException {
    public CannotManageOwnClinicException() {
        super("An admin cannot suspend or reactivate a clinic where they are an employee or owner");
    }
}
