package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.DeclineInvitationUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Rechazar cierra la invitación sin crear ningún empleado. Aplica exactamente las mismas reglas de
 * identidad y estado que aceptar ({@link InvitationGate}), y no publica evento: la lista de eventos
 * de personal del plan no contempla uno para el rechazo, y la clínica lo ve igual en el listado de
 * invitaciones (la fila pasa a {@code DECLINED}).
 *
 * <p>A diferencia de aceptar, no exige que la clínica esté {@code ACTIVE}: poder sacarse de encima
 * una invitación nunca debería depender del estado de la clínica que invita.
 */
@Service
@RequiredArgsConstructor
public class DeclineInvitationService implements DeclineInvitationUseCase {

    private final EmployeeInvitationRepositoryPort invitationRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    @Transactional
    public DeclineInvitationResponse decline(DeclineInvitationRequest request) {
        AuthenticatedUser caller = authenticatedUserPort.current();
        LocalDateTime now = LocalDateTime.now();

        EmployeeInvitation invitation = InvitationGate.requireUsableBy(
                invitationRepositoryPort, caller, InvitationGate.hashOf(request.getToken()), now);

        EmployeeInvitation declined = invitationRepositoryPort.save(invitation.toBuilder()
                .status(InvitationStatus.DECLINED)
                .decidedAt(now)
                .build());

        return DeclineInvitationResponse.builder()
                .invitationId(declined.getId())
                .veterinaryId(declined.getVeterinaryId())
                .status(declined.getStatus())
                .decidedAt(declined.getDecidedAt())
                .build();
    }
}
