package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkToVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * El dueño se vincula a una clínica con su código. Token <strong>normal</strong>: todavía no tiene
 * ninguna relación con la clínica, así que no existe un token de clínica para esto.
 *
 * <p><strong>Idempotente</strong>: si ya está vinculado devuelve {@code 200} con
 * {@code alreadyLinked = true} y no escribe nada —ni publica evento— en vez del {@code 409} que
 * daban {@code LinkByCode}/{@code LinkByUrl}. Un dueño que toca dos veces el botón, o un frontend
 * que reintenta un POST cuya respuesta se perdió, no debería ver un error.
 *
 * <p>Vincularse <strong>no</strong> le da a la clínica acceso a ninguna mascota: el consentimiento
 * es por mascota y explícito ({@code SharePetsWithVeterinary}). Lo único que la clínica ve al
 * vincularse es el nombre, correo y teléfono del dueño — y por eso la pantalla que llama a esto
 * tiene que decírselo antes (ver {@code docs/FASE_4A_VINCULACION.md}).
 */
@Service
@RequiredArgsConstructor
public class LinkToVeterinaryService implements LinkToVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final UserServicePort userServicePort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public LinkToVeterinaryResponse link(LinkToVeterinaryRequest request) {
        AuthenticatedUser caller = authenticatedUserPort.current();
        Veterinary veterinary = LinkCodes.resolveActiveClinic(veterinaryRepositoryPort, request.getCode());

        Optional<UserVeterinaryLink> active = linkRepositoryPort.findActiveLink(caller.userId(), veterinary.getId());
        if (active.isPresent()) {
            return toResponse(active.get(), veterinary, true);
        }

        UserVeterinaryLink saved = linkRepositoryPort.save(
                upsertLink(caller, veterinary.getId(), LocalDateTime.now()));

        publishLinkedEvent(veterinary, saved);

        return toResponse(saved, veterinary, false);
    }

    /**
     * Reactiva la fila que ya existía si el dueño se había desvinculado antes (mismo {@code id}),
     * en vez de insertar otra: el índice único parcial {@code ux_link_active_user_vet} no admite dos
     * filas {@code LINKED} del par, y así tampoco se pierde el historial.
     */
    private UserVeterinaryLink upsertLink(AuthenticatedUser caller, UUID veterinaryId, LocalDateTime now) {
        UserVeterinaryLink base = linkRepositoryPort.findLatestLink(caller.userId(), veterinaryId)
                .map(existing -> existing.toBuilder().build())
                .orElseGet(() -> UserVeterinaryLink.builder()
                        .id(UUID.randomUUID())
                        .userId(caller.userId())
                        .veterinaryId(veterinaryId)
                        .build());

        return base.toBuilder()
                .status(LinkStatus.LINKED)
                .linkedAt(now)
                .unlinkedAt(null)
                // El snapshot se refresca al (re)vincularse: si venía de una desvinculación, el
                // correo y el teléfono habían sido borrados por UnlinkService (Ley 1581 de 2012).
                .ownerName(resolveName(caller.userId()))
                .ownerEmail(caller.email())
                .ownerPhone(null)
                .build();
    }

    /**
     * {@code ownerEmail} sale del token, que es la fuente confiable y no cuesta una llamada.
     * {@code ownerName} es best-effort ({@code getUsers} nunca lanza y el nombre puede venir
     * {@code null}).
     *
     * <p>{@code ownerPhone} queda siempre {@code null}: el contrato interno del User service
     * ({@code GET /internal/users/{id}} → {@code id, email, emailVerified, name}) no expone el
     * teléfono. La columna existe porque el plan §7.3 contempla buscar clientes por teléfono exacto;
     * se podrá llenar cuando el User service lo devuelva (ver "Pendientes del lado del User service"
     * en CLAUDE.md).
     */
    private String resolveName(UUID userId) {
        return Optional.ofNullable(userServicePort.getUsers(List.of(userId)).get(userId))
                .map(UserServicePort.UserInfo::name)
                .orElse(null);
    }

    /** Avisa al OWNER activo de la clínica que tiene un cliente nuevo. */
    private void publishLinkedEvent(Veterinary veterinary, UserVeterinaryLink link) {
        employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinary.getId(), EmployeeRole.OWNER)
                .ifPresent(owner -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("veterinaryName", veterinary.getName());
                    payload.put("clientUserId", link.getUserId());
                    payload.put("clientName", link.getOwnerName());
                    payload.put("clientEmail", link.getOwnerEmail());
                    payload.put("linkedAt", link.getLinkedAt());
                    eventPublisherPort.publish(DomainEvent.create(
                            VeterinaryEventType.USER_LINKED,
                            owner.getUserId(),
                            null,
                            payload,
                            EventMetadata.of(veterinary.getId(), null)));
                });
    }

    private LinkToVeterinaryResponse toResponse(UserVeterinaryLink link, Veterinary veterinary,
                                                 boolean alreadyLinked) {
        return LinkToVeterinaryResponse.builder()
                .id(link.getId())
                .veterinaryId(veterinary.getId())
                .veterinaryName(veterinary.getName())
                .logoUrl(veterinary.getLogoUrl())
                .status(link.getStatus())
                .linkedAt(link.getLinkedAt())
                .alreadyLinked(alreadyLinked)
                .build();
    }
}
