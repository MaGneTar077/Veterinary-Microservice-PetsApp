package com.MyAnimaLog.Veterinary.domain.staff.exceptions;

/**
 * Solo el OWNER puede crear o sostener ADMINs: un ADMIN no puede invitar con rol ADMIN,
 * reenviar una invitación de ADMIN, ni promover a otro empleado a ADMIN. Complementa a
 * {@link CannotModifyPeerAdminException}, que cubre el caso de un ADMIN tocando a otro ADMIN
 * que ya existe.
 */
public class OnlyOwnerCanManageAdminsException extends RuntimeException {

    public OnlyOwnerCanManageAdminsException() {
        super("Only the clinic OWNER can manage ADMIN staff");
    }
}
