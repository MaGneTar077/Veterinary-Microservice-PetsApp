package com.MyAnimaLog.Veterinary.domain.patients.exceptions;

/**
 * El caller no es el dueño de la mascota en Pets — o la mascota no existe.
 *
 * <p>Los dos casos dan el <strong>mismo 403</strong> a propósito: un 404 para "no existe" le
 * confirmaría a cualquiera, probando UUIDs, qué mascotas hay registradas en la plataforma.
 *
 * <p>Solo el dueño comparte: un EDITOR o VIEWER de la mascota (los roles de compartir entre
 * personas que maneja Pets) <strong>nunca</strong> puede darle acceso a una clínica. Ceder datos
 * clínicos a un tercero es una decisión del dueño.
 */
public class PetNotOwnedException extends RuntimeException {

    public PetNotOwnedException() {
        super("The pet does not belong to the current user");
    }

    public PetNotOwnedException(String message) {
        super(message);
    }
}
