package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/**
 * La invitación no existe, o no pertenece a la clínica de la ruta. El mensaje es el mismo en
 * ambos casos a propósito: no revela si un id de invitación existe en otra clínica.
 */
public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException() {
        super("Invitation not found");
    }
}
