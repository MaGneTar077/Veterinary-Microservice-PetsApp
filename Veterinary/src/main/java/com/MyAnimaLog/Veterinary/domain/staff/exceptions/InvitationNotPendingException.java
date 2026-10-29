package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/**
 * La invitación ya fue aceptada, rechazada o revocada, así que no admite más acciones
 * (reenviar, revocar, aceptar, rechazar). El vencimiento es un caso aparte
 * ({@link InvitationExpiredException}): una invitación vencida sigue siendo PENDING y
 * se puede reenviar.
 */
public class InvitationNotPendingException extends RuntimeException {

    public InvitationNotPendingException() {
        super("Invitation is no longer pending");
    }
}
