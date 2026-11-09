package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Corta la relación entre un dueño y una clínica, desde cualquiera de los dos lados: el dueño sobre
 * sí mismo, o el personal con {@code CLINIC_CONFIGURE} sobre un cliente
 * ({@code requireSelfOrPermission}). Se usa {@code CLINIC_CONFIGURE} y no {@code STAFF_MANAGE}
 * porque el vinculado es un cliente, no un empleado.
 *
 * <p>Tres cosas pasan en <strong>la misma transacción</strong>, y ninguna puede quedar a medias:
 * <ol>
 *   <li><strong>Se revocan TODAS las mascotas</strong> que ese dueño compartía con esa clínica. Es
 *       lo más importante: si la vinculación se cortara sin revocar, la clínica seguiría teniendo
 *       acceso a las historias clínicas por el endpoint {@code access}, que mira
 *       {@code veterinary_patients} y no la vinculación.</li>
 *   <li>La vinculación pasa a {@code UNLINKED} + {@code unlinkedAt}. No se borra la fila: hay que
 *       poder explicar después por qué la clínica dejó de ver a ese cliente.</li>
 *   <li>Se <strong>borran del snapshot el correo y el teléfono</strong> del dueño, conservando solo
 *       el nombre (Ley 1581 de 2012: la clínica ya no tiene una razón para guardar el contacto de
 *       alguien que cortó la relación, pero sin el nombre el histórico sería ilegible).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class UnlinkService implements UnlinkUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;
    private final VeterinaryPatientRepositoryPort patientRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public UnlinkResponse unlink(UUID userId, UUID veterinaryId) {
        authorizationService.requireSelfOrPermission(userId, veterinaryId, Permission.CLINIC_CONFIGURE);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        UserVeterinaryLink link = linkRepositoryPort.findActiveLink(userId, veterinaryId)
                .orElseThrow(UserNotLinkedException::new);

        LocalDateTime now = LocalDateTime.now();
        int revokedPets = revokeSharedPets(veterinaryId, userId, now);

        linkRepositoryPort.save(link.toBuilder()
                .status(LinkStatus.UNLINKED)
                .unlinkedAt(now)
                .ownerEmail(null)
                .ownerPhone(null)
                .build());

        publishUnlinkedEvent(veterinary, link, userId, revokedPets);

        return UnlinkResponse.builder()
                .userId(userId)
                .veterinaryId(veterinaryId)
                .revokedPetsCount(revokedPets)
                .message("User unlinked successfully")
                .build();
    }

    /**
     * Guarda fila por fila y no con un {@code UPDATE} masivo a propósito: {@code @UpdateTimestamp}
     * de {@code VeterinaryPatientEntity} no se aplica a las sentencias masivas de JPQL, así que un
     * bulk update dejaría {@code updated_at} viejo en silencio. N es chico (las mascotas que una
     * persona comparte con una sola clínica).
     */
    private int revokeSharedPets(UUID veterinaryId, UUID userId, LocalDateTime now) {
        List<VeterinaryPatient> shared =
                patientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId);
        shared.forEach(patient -> patientRepositoryPort.save(patient.toBuilder()
                .consentStatus(ConsentStatus.REVOKED)
                .revokedAt(now)
                .build()));
        return shared.size();
    }

    /**
     * Va al OWNER activo de la clínica (es quien necesita saber que perdió el acceso a esos
     * pacientes). {@code initiatedByClient} le dice a Notifications cómo redactarlo: el dueño se fue
     * por su cuenta, o la propia clínica lo quitó de su lista — mismo criterio que
     * {@code EMPLOYEE_DEACTIVATED.leftVoluntarily} de la Fase 3.
     */
    private void publishUnlinkedEvent(Veterinary veterinary, UserVeterinaryLink link, UUID userId,
                                       int revokedPets) {
        boolean initiatedByClient = authenticatedUserPort.current().userId().equals(userId);
        employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinary.getId(), EmployeeRole.OWNER)
                .ifPresent(owner -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("veterinaryName", veterinary.getName());
                    payload.put("clientUserId", userId);
                    payload.put("clientName", link.getOwnerName());
                    payload.put("revokedPetsCount", revokedPets);
                    payload.put("initiatedByClient", initiatedByClient);
                    eventPublisherPort.publish(DomainEvent.create(
                            VeterinaryEventType.USER_UNLINKED,
                            owner.getUserId(),
                            null,
                            payload,
                            EventMetadata.of(veterinary.getId(), null)));
                });
    }
}
