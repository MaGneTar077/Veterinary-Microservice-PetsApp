package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.staff.dto.InvitationSummary;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;

import java.time.LocalDateTime;

/**
 * Convierte una {@link EmployeeInvitation} en el DTO que ve la clínica. Utilidad estática (no es un
 * puerto ni un servicio), escrita una sola vez para que invitar, listar, reenviar y revocar devuelvan
 * exactamente la misma forma — y para que el {@code tokenHash} no se pueda filtrar por descuido en
 * una sola de las cuatro respuestas.
 */
public final class InvitationSummaries {

    private InvitationSummaries() {
    }

    public static InvitationSummary of(EmployeeInvitation invitation, LocalDateTime now) {
        return InvitationSummary.builder()
                .id(invitation.getId())
                .veterinaryId(invitation.getVeterinaryId())
                .email(invitation.getEmail())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .expired(invitation.isExpired(now))
                .invitedBy(invitation.getInvitedBy())
                .expiresAt(invitation.getExpiresAt())
                .decidedAt(invitation.getDecidedAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
