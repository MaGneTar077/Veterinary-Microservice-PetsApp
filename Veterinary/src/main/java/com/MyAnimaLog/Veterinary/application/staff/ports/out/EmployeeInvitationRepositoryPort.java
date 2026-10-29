package com.MyAnimaLog.Veterinary.application.staff.ports.out;

import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeInvitationRepositoryPort {

    EmployeeInvitation save(EmployeeInvitation invitation);

    Optional<EmployeeInvitation> findById(UUID id);

    /** Búsqueda por el hash del token (nunca por el token en claro) para aceptar/rechazar/previsualizar. */
    Optional<EmployeeInvitation> findByTokenHash(String tokenHash);

    /** Invitaciones de una clínica en un estado concreto, más recientes primero. */
    List<EmployeeInvitation> findByVeterinaryIdAndStatus(UUID veterinaryId, InvitationStatus status);

    /**
     * La invitación de esa clínica + correo en un estado concreto. Con {@code PENDING} devuelve como
     * máximo una fila, garantizado por el índice único parcial {@code ux_pending_invitation_per_vet_email}
     * (puede estar vencida: el vencimiento no cambia el estado).
     */
    Optional<EmployeeInvitation> findByVeterinaryIdAndEmailAndStatus(UUID veterinaryId, String email,
                                                                      InvitationStatus status);

    /** Invitaciones dirigidas a un correo, para {@code GET /api/veterinary/me/invitations}. */
    List<EmployeeInvitation> findByEmailAndStatus(String email, InvitationStatus status);
}
