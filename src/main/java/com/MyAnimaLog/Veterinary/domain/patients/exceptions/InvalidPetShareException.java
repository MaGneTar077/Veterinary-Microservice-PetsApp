package com.MyAnimaLog.Veterinary.domain.patients.exceptions;

/** La petición de compartir está malformada (sin mascotas, o con un id nulo en la lista). */
public class InvalidPetShareException extends RuntimeException {

    public InvalidPetShareException(String message) {
        super(message);
    }
}
