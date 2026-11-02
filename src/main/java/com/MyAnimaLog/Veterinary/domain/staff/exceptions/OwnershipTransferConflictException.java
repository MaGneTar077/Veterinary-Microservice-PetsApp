package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/**
 * Alguien cambió los roles involucrados mientras la transferencia estaba en curso: uno de los dos
 * {@code UPDATE} condicionales no actualizó ninguna fila (dos transferencias simultáneas, o al
 * destino le cambiaron el rol en el medio). La transacción se revierte completa: la clínica nunca
 * queda sin OWNER ni con dos.
 */
public class OwnershipTransferConflictException extends RuntimeException {

    public OwnershipTransferConflictException() {
        super("Ownership changed while the transfer was in progress; retry");
    }
}
