package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/** El correo de la invitación falta o no tiene un formato válido. */
public class InvalidInvitationEmailException extends RuntimeException {

    public InvalidInvitationEmailException() {
        super("A valid email is required to invite someone");
    }
}
