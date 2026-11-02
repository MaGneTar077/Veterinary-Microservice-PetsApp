package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/**
 * La invitación venció ({@code expires_at < now()}). Sigue en estado PENDING, así que quien
 * invitó puede reenviarla con un token nuevo; el invitado no puede usarla.
 */
public class InvitationExpiredException extends RuntimeException {

    public InvitationExpiredException() {
        super("Invitation has expired");
    }
}
