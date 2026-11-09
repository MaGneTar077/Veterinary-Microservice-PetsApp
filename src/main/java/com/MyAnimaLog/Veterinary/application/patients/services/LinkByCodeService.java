package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeResponse;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkByCodeUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkToVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Alias de {@link LinkToVeterinaryUseCase} para la ruta vieja {@code POST /api/veterinary/link/code}.
 *
 * @deprecated Usar {@code POST /api/veterinary/links} ({@code LinkToVeterinary}). Delega en el
 * servicio nuevo en vez de conservar su propia copia de la lógica, así que <strong>hereda su
 * comportamiento</strong> y eso cambia dos respuestas respecto de la Fase 3:
 * <ul>
 *   <li>Un usuario ya vinculado recibe {@code 200} con la vinculación existente, no
 *       {@code 409 USER_ALREADY_LINKED}.</li>
 *   <li>Un código que no existe, o cuya clínica no está {@code ACTIVE}, recibe
 *       {@code 404 LINK_CODE_NOT_FOUND} en vez de {@code 400 INVALID_INVITE_CODE} /
 *       {@code 409 VETERINARY_NOT_ACTIVE}.</li>
 * </ul>
 * Era eso o mantener dos caminos de vinculación con reglas distintas sobre la misma tabla, que es
 * peor: el que no se toca es el que se olvida (p. ej. este nunca revocaría mascotas al re-vincular,
 * ni refrescaría el snapshot del dueño).
 */
@Deprecated(since = "Fase 4A", forRemoval = true)
@Service
@RequiredArgsConstructor
public class LinkByCodeService implements LinkByCodeUseCase {

    private final LinkToVeterinaryUseCase linkToVeterinaryUseCase;

    /** Solo para rellenar el {@code userId} que la respuesta vieja incluía; el nuevo DTO no lo trae
     * porque la vinculación es siempre del dueño del token. */
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    public LinkByCodeResponse linkByCode(LinkByCodeRequest request) {
        LinkToVeterinaryResponse response = linkToVeterinaryUseCase.link(
                LinkToVeterinaryRequest.builder().code(request.getInviteCode()).build());

        return LinkByCodeResponse.builder()
                .id(response.getId())
                .userId(authenticatedUserPort.current().userId())
                .veterinaryId(response.getVeterinaryId())
                .status(response.getStatus() == null ? null : response.getStatus().name())
                .linkedAt(response.getLinkedAt())
                .build();
    }
}
