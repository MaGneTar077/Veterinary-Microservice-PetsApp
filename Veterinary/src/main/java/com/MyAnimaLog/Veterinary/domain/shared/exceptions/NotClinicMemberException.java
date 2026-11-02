package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

/**
 * El token de clínica es válido y apunta a la clínica correcta, pero en la base el caller ya no es
 * un empleado activo de esa clínica: fue retirado, o se fue por su cuenta, después de que se emitió
 * el token (que vive hasta 60 min). Nunca se confía en el claim para esto.
 */
public class NotClinicMemberException extends RuntimeException {

    public NotClinicMemberException() {
        super("You are no longer an active member of this veterinary");
    }
}
