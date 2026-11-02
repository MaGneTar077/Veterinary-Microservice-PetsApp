package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/**
 * El correo del token no es el correo al que se envió la invitación. El mensaje no dice a qué
 * correo fue dirigida: eso solo se revela en la vista previa, que exige tener el token.
 */
public class InvitationEmailMismatchException extends RuntimeException {

    public InvitationEmailMismatchException() {
        super("This invitation was sent to a different email address");
    }
}
