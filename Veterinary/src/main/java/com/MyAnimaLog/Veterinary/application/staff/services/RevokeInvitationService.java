package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.RevokeInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.RevokeInvitationUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Revoca una invitación pendiente (vigente o vencida). Cualquiera con {@code STAFF_MANAGE} puede
 * revocar, incluida una invitación de ADMIN: revocar nunca escala privilegios, así que no aplica la
 * restricción de "solo el OWNER" que sí tienen invitar y reenviar.
 */
@Service
@RequiredArgsConstructor
public class RevokeInvitationService implements RevokeInvitationUseCase {

    private final EmployeeInvitationRepositoryPort invitationRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    @Transactional
    public RevokeInvitationResponse revoke(UUID veterinaryId, UUID invitationId) {
        authorizationService.require(veterinaryId, Permission.STAFF_MANAGE);

        EmployeeInvitation invitation = invitationRepositoryPort.findById(invitationId)
                .filter(candidate -> candidate.getVeterinaryId().equals(veterinaryId))
                .orElseThrow(InvitationNotFoundException::new);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationNotPendingException();
        }

        LocalDateTime now = LocalDateTime.now();
        EmployeeInvitation revoked = invitationRepositoryPort.save(invitation.toBuilder()
                .status(InvitationStatus.REVOKED)
                .decidedAt(now)
                .build());

        return RevokeInvitationResponse.builder()
                .invitation(InvitationSummaries.of(revoked, now))
                .build();
    }
}
