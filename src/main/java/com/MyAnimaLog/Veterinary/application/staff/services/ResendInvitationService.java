package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.ResendInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ResendInvitationUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.infrastructure.security.InvitationTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reenvía una invitación: token nuevo, vencimiento nuevo y otro evento {@code EMPLOYEE_INVITED}.
 * El token anterior deja de servir en el mismo momento (su hash se sobrescribe), así que un enlace
 * viejo que ya estaba en un correo queda inutilizable.
 *
 * <p>Funciona tanto sobre una invitación vigente como sobre una vencida: el vencimiento no cambia el
 * estado, y reenviar es justamente la salida para una vencida. Lo que no se puede reenviar es una
 * invitación ya aceptada, rechazada o revocada.
 */
@Service
@RequiredArgsConstructor
public class ResendInvitationService implements ResendInvitationUseCase {

    private static final int EXPIRATION_DAYS = 7;

    private final EmployeeInvitationRepositoryPort invitationRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final InvitationNotifier invitationNotifier;

    @Override
    @Transactional
    public ResendInvitationResponse resend(UUID veterinaryId, UUID invitationId) {
        authorizationService.require(veterinaryId, Permission.STAFF_MANAGE);

        EmployeeInvitation invitation = invitationRepositoryPort.findById(invitationId)
                .filter(candidate -> candidate.getVeterinaryId().equals(veterinaryId))
                .orElseThrow(InvitationNotFoundException::new);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationNotPendingException();
        }

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);
        if (veterinary.getStatus() != VeterinaryStatus.ACTIVE) {
            throw new ClinicNotActiveException();
        }

        AuthenticatedUser caller = authenticatedUserPort.current();
        VeterinaryEmployee actor = employeeRepositoryPort
                .findByVeterinaryIdAndUserId(veterinaryId, caller.userId())
                .orElseThrow(EmployeeNotFoundException::new);
        // Misma regla que al invitar: un ADMIN no puede mantener viva una invitación de ADMIN.
        if (invitation.getRole() == EmployeeRole.ADMIN && actor.getRole() != EmployeeRole.OWNER) {
            throw new OnlyOwnerCanManageAdminsException();
        }

        LocalDateTime now = LocalDateTime.now();
        String clearToken = InvitationTokens.generate();
        EmployeeInvitation resent = invitationRepositoryPort.save(invitation.toBuilder()
                .tokenHash(InvitationTokens.hash(clearToken))
                .expiresAt(now.plusDays(EXPIRATION_DAYS))
                .build());

        invitationNotifier.publishInvited(resent, clearToken, veterinary);

        return ResendInvitationResponse.builder()
                .invitation(InvitationSummaries.of(resent, now))
                .build();
    }
}
