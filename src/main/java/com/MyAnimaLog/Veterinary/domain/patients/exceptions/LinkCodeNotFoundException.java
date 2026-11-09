package com.MyAnimaLog.Veterinary.domain.patients.exceptions;

/**
 * El código de vinculación no resuelve a una clínica que pueda recibir clientes: no existe, o
 * existe pero la clínica no está {@code ACTIVE}.
 *
 * <p>Los dos casos dan el <strong>mismo 404 con el mismo mensaje a propósito</strong>: con un código
 * de 8 caracteres, distinguir "no existe" de "existe pero no está activa" le permitiría a cualquiera
 * ir probando códigos y enumerar qué clínicas están registradas y en qué estado.
 *
 * <p>No reemplaza a {@link InvalidInviteCodeException} (400): esa es para una petición malformada
 * —sin código— y esta para un código que simplemente no coincide con nada utilizable.
 */
public class LinkCodeNotFoundException extends RuntimeException {

    public LinkCodeNotFoundException() {
        super("No active veterinary matches that link code");
    }

    public LinkCodeNotFoundException(String message) {
        super(message);
    }
}
