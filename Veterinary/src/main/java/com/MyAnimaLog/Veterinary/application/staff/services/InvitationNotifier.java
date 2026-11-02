package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.dto.Recipient;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Publica el evento {@code EMPLOYEE_INVITED} con el enlace de aceptación. No implementa ningún
 * {@code *UseCase}: es un colaborador interno compartido por {@code InviteEmployeeService} y
 * {@code ResendInvitationService}, escrito una sola vez para que el formato del enlace y el payload
 * no se separen entre crear y reenviar (mismo patrón que {@code ProfessionalProfileSubmissionService}).
 *
 * <p>El destinatario es el {@code recipient.email} de la invitación, no un {@code userId}: el invitado
 * puede no tener cuenta todavía. El token en claro viaja únicamente aquí, dentro del payload del
 * evento; nunca se loguea (los logs de este servicio solo mencionan el id de la invitación).
 */
@Component
@RequiredArgsConstructor
public class InvitationNotifier {

    private final EventPublisherPort eventPublisherPort;

    @Value("${app.invitation.base-url}")
    private String invitationBaseUrl;

    public void publishInvited(EmployeeInvitation invitation, String clearToken, Veterinary veterinary) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("veterinaryId", veterinary.getId());
        payload.put("veterinaryName", veterinary.getName());
        payload.put("role", invitation.getRole().name());
        payload.put("acceptUrl", acceptUrl(clearToken));
        payload.put("expiresAt", invitation.getExpiresAt());

        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.EMPLOYEE_INVITED,
                null,
                new Recipient(invitation.getEmail(), null, null),
                payload,
                EventMetadata.of(veterinary.getId(), null)));
    }

    private String acceptUrl(String clearToken) {
        String separator = invitationBaseUrl != null && invitationBaseUrl.contains("?") ? "&" : "?";
        return invitationBaseUrl + separator + "token=" + clearToken;
    }
}
