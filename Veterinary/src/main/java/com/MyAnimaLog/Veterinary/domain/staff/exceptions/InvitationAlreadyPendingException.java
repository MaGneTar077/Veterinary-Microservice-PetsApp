package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/** Ya hay una invitación PENDING y vigente para esa clínica + correo. */
public class InvitationAlreadyPendingException extends RuntimeException {

    public InvitationAlreadyPendingException() {
        super("There is already a pending invitation for this email in this veterinary");
    }
}
