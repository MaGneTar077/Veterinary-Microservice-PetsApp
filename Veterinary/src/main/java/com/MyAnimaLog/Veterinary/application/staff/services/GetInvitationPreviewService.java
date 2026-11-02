package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.GetInvitationPreviewUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pantalla que ve el invitado al abrir el enlace, antes de decidir. Devuelve {@code 200} incluso
 * cuando la invitación ya no sirve (vencida, revocada, usada) o cuando el correo de la sesión no es
 * el invitado: son estados que el frontend tiene que pintar, no errores. El único {@code 404} es un
 * token que no existe.
 */
@Service
@RequiredArgsConstructor
public class GetInvitationPreviewService implements GetInvitationPreviewUseCase {

    private final EmployeeInvitationRepositoryPort invitationRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final UserServicePort userServicePort;

    @Override
    public GetInvitationPreviewResponse preview(GetInvitationPreviewRequest request) {
        AuthenticatedUser caller = authenticatedUserPort.current();
        LocalDateTime now = LocalDateTime.now();

        EmployeeInvitation invitation = invitationRepositoryPort
                .findByTokenHash(InvitationGate.hashOf(request.getToken()))
                .orElseThrow(InvitationNotFoundException::new);

        Optional<Veterinary> veterinary = veterinaryRepositoryPort.findById(invitation.getVeterinaryId());
        boolean emailMatchesCaller = InvitationGate.matchesCaller(invitation, caller);

        return GetInvitationPreviewResponse.builder()
                .invitationId(invitation.getId())
                .veterinaryId(invitation.getVeterinaryId())
                .veterinaryName(veterinary.map(Veterinary::getName).orElse(null))
                .veterinaryCity(veterinary.map(Veterinary::getCity).orElse(null))
                .logoUrl(veterinary.map(Veterinary::getLogoUrl).orElse(null))
                .role(invitation.getRole())
                .invitedByName(resolveName(invitation.getInvitedBy()))
                .email(invitation.getEmail())
                .emailMatchesCaller(emailMatchesCaller)
                .status(invitation.getStatus())
                .expired(invitation.isExpired(now))
                .usable(invitation.isUsable(now) && emailMatchesCaller && caller.emailVerified())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    /** Best-effort: {@code getUsers} nunca lanza, y el nombre puede no venir (ver UserServicePort). */
    private String resolveName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return Optional.ofNullable(userServicePort.getUsers(List.of(userId)).get(userId))
                .map(UserServicePort.UserInfo::name)
                .orElse(null);
    }
}
