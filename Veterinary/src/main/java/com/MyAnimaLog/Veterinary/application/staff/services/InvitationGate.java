package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationExpiredException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.infrastructure.security.InvitationTokens;

import java.time.LocalDateTime;

/**
 * Resuelve un token de invitación y valida quién lo está usando. Utilidad estática (no es un puerto
 * ni un servicio), compartida por {@code AcceptInvitationService} y {@code DeclineInvitationService}
 * para que las dos acciones del invitado apliquen exactamente las mismas reglas de identidad y
 * estado — aceptar y rechazar no pueden tener criterios distintos sobre de quién es la invitación.
 *
 * <p>Orden de las validaciones, a propósito: primero identidad (¿es tuya esta invitación?), después
 * estado (¿sirve todavía?). Así alguien con un token que no le corresponde recibe siempre el mismo
 * 403 y no puede deducir del código de error si la invitación existe, venció o ya se usó.
 *
 * <p>El token nunca se loguea ni se incluye en el mensaje de ninguna de estas excepciones.
 */
public final class InvitationGate {

    private InvitationGate() {
    }

    /**
     * Hash del token recibido. Un token ausente o vacío se trata como un token inexistente
     * ({@code 404}) y no como un error de validación: así la respuesta es idéntica a la de un token
     * cualquiera que no está en la base.
     */
    public static String hashOf(String token) {
        if (token == null || token.isBlank()) {
            throw new InvitationNotFoundException();
        }
        return InvitationTokens.hash(token.trim());
    }

    public static EmployeeInvitation requireUsableBy(EmployeeInvitationRepositoryPort invitationRepositoryPort,
                                                      AuthenticatedUser caller,
                                                      String tokenHash,
                                                      LocalDateTime now) {
        EmployeeInvitation invitation = invitationRepositoryPort.findByTokenHash(tokenHash)
                .orElseThrow(InvitationNotFoundException::new);

        if (!matchesCaller(invitation, caller)) {
            throw new InvitationEmailMismatchException();
        }
        if (!caller.emailVerified()) {
            throw new EmailNotVerifiedException();
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationNotPendingException();
        }
        if (invitation.isExpired(now)) {
            throw new InvitationExpiredException();
        }
        return invitation;
    }

    /** Comparación del correo del token con el de la invitación, sin distinguir mayúsculas. */
    public static boolean matchesCaller(EmployeeInvitation invitation, AuthenticatedUser caller) {
        return caller.email() != null
                && invitation.getEmail() != null
                && caller.email().trim().equalsIgnoreCase(invitation.getEmail());
    }
}
