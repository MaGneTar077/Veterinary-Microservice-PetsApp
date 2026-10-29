package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.dto.GetMyInvitationsResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.GetMyInvitationsUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Invitaciones pendientes y vigentes para el correo del token. El correo sale del token, nunca de un
 * parámetro: nadie puede listar las invitaciones de otra persona.
 */
@Service
@RequiredArgsConstructor
public class GetMyInvitationsService implements GetMyInvitationsUseCase {

    private final EmployeeInvitationRepositoryPort invitationRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    public GetMyInvitationsResponse myInvitations() {
        AuthenticatedUser caller = authenticatedUserPort.current();
        if (caller.email() == null || caller.email().isBlank()) {
            return GetMyInvitationsResponse.builder().invitations(List.of()).build();
        }

        LocalDateTime now = LocalDateTime.now();
        List<EmployeeInvitation> pending = invitationRepositoryPort
                .findByEmailAndStatus(caller.email().trim().toLowerCase(), InvitationStatus.PENDING).stream()
                .filter(invitation -> !invitation.isExpired(now))
                .toList();

        // Una sola consulta por lote para los nombres/logos de las clínicas, en vez de una por fila.
        Map<UUID, Veterinary> clinics = pending.isEmpty()
                ? Map.of()
                : veterinaryRepositoryPort.findByIdIn(pending.stream()
                        .map(EmployeeInvitation::getVeterinaryId)
                        .distinct()
                        .toList()).stream()
                .collect(Collectors.toMap(Veterinary::getId, Function.identity()));

        List<GetMyInvitationsResponse.MyInvitation> invitations = pending.stream()
                .map(invitation -> {
                    Veterinary clinic = clinics.get(invitation.getVeterinaryId());
                    return GetMyInvitationsResponse.MyInvitation.builder()
                            .invitationId(invitation.getId())
                            .veterinaryId(invitation.getVeterinaryId())
                            .veterinaryName(clinic != null ? clinic.getName() : null)
                            .veterinaryCity(clinic != null ? clinic.getCity() : null)
                            .logoUrl(clinic != null ? clinic.getLogoUrl() : null)
                            .role(invitation.getRole())
                            .expiresAt(invitation.getExpiresAt())
                            .build();
                })
                .toList();

        return GetMyInvitationsResponse.builder().invitations(invitations).build();
    }
}
