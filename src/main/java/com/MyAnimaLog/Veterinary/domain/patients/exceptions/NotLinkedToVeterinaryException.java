package com.MyAnimaLog.Veterinary.domain.patients.exceptions;

/**
 * El dueño intentó compartir una mascota con una clínica a la que no está vinculado.
 *
 * <p>409 y no 404: la clínica existe y la mascota es suya, lo que falta es un paso previo del flujo
 * (vincularse con el código de la clínica). Un 404 diría "no existe", que es falso y manda al
 * frontend a mostrar el error equivocado. Tampoco se reusa {@code UserNotLinkedException}, que es
 * el 404 de desvincularse de algo que no está vinculado.
 */
public class NotLinkedToVeterinaryException extends RuntimeException {

    public NotLinkedToVeterinaryException() {
        super("You must be linked to this veterinary before sharing pets with it");
    }

    public NotLinkedToVeterinaryException(String message) {
        super(message);
    }
}
