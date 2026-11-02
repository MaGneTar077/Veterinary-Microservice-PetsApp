package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/**
 * El OWNER no puede irse de su propia clínica: dejaría una clínica sin dueño (y el índice
 * {@code ux_one_owner_per_vet} garantiza que hay exactamente uno). Primero tiene que transferir la
 * propiedad con {@code POST /api/veterinary/{id}/ownership-transfer}.
 */
public class OwnerCannotLeaveException extends RuntimeException {

    public OwnerCannotLeaveException() {
        super("The OWNER must transfer ownership before leaving the veterinary");
    }
}
