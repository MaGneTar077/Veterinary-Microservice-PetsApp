package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlResponse;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkByUrlUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkToVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.LinkCodeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Alias de {@link LinkToVeterinaryUseCase} para la ruta vieja {@code POST /api/veterinary/link/url}.
 *
 * <p>Resuelve el enlace a su clínica y delega con el código de esa clínica, en vez de parsear el
 * {@code code=} de la URL: así el enlace sigue teniendo que coincidir con uno realmente emitido, y
 * todas las reglas (clínica {@code ACTIVE}, idempotencia, snapshot del dueño, evento) salen de un
 * solo lugar.
 *
 * @deprecated Usar {@code POST /api/veterinary/links} con el código. Un enlace completo en el cuerpo
 * no aporta nada sobre el código que ya contiene, y hereda los mismos cambios de respuesta que
 * {@link LinkByCodeService} (ya vinculado → {@code 200}; enlace desconocido o clínica no
 * {@code ACTIVE} → {@code 404 LINK_CODE_NOT_FOUND}).
 */
@Deprecated(since = "Fase 4A", forRemoval = true)
@Service
@RequiredArgsConstructor
public class LinkByUrlService implements LinkByUrlUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final LinkToVeterinaryUseCase linkToVeterinaryUseCase;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    public LinkByUrlResponse linkByUrl(LinkByUrlRequest request) {
        if (request.getInviteLink() == null || request.getInviteLink().isBlank()) {
            throw new InvalidInviteCodeException("Invite link is required");
        }

        Veterinary veterinary = veterinaryRepositoryPort.findByInviteLink(request.getInviteLink())
                .orElseThrow(LinkCodeNotFoundException::new);

        LinkToVeterinaryResponse response = linkToVeterinaryUseCase.link(
                LinkToVeterinaryRequest.builder().code(veterinary.getInviteCode()).build());

        return LinkByUrlResponse.builder()
                .id(response.getId())
                .userId(authenticatedUserPort.current().userId())
                .veterinaryId(response.getVeterinaryId())
                .status(response.getStatus() == null ? null : response.getStatus().name())
                .linkedAt(response.getLinkedAt())
                .build();
    }
}
