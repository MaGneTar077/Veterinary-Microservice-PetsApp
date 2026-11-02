package com.MyAnimaLog.Veterinary.domain.staff.enums;

/**
 * Estado de una invitación de personal ({@code employee_invitations.status}).
 *
 * <p>No existe un estado {@code EXPIRED}: el vencimiento se deriva siempre de
 * {@code expires_at < now()} (ver {@code EmployeeInvitation#isExpired}), así no hace falta un job
 * que mueva filas. Una {@code PENDING} vencida la pasa a {@code REVOKED} {@code InviteEmployeeService}
 * cuando se reinvita al mismo correo, porque si no bloquearía el índice único parcial
 * {@code ux_pending_invitation_per_vet_email}.
 */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    REVOKED
}
