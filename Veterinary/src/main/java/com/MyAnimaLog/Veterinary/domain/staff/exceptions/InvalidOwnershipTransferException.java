package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/**
 * El destino de la transferencia de propiedad no sirve: falta el {@code newOwnerEmployeeId}, es el
 * OWNER actual, o es un empleado retirado.
 */
public class InvalidOwnershipTransferException extends RuntimeException {

    public InvalidOwnershipTransferException(String message) {
        super(message);
    }
}
