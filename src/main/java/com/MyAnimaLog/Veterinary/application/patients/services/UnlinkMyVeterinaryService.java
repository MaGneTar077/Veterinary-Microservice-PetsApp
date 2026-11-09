package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkMyVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkUseCase;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * "Desvincularme de esta clínica", con el {@code userId} del token en vez de la ruta.
 *
 * <p>No reimplementa nada: delega en {@link UnlinkUseCase}, que es donde vive la revocación en
 * cascada de las mascotas y el borrado del contacto del snapshot. Tener dos caminos con su propia
 * copia de esa lógica es exactamente cómo se termina con una desvinculación que, por un lado, deja
 * mascotas compartidas.
 *
 * <p>Existe como caso de uso aparte (y no resolviendo el token en el controlador) para que el
 * controlador siga sin tocar puertos, como el resto del servicio.
 */
@Service
@RequiredArgsConstructor
public class UnlinkMyVeterinaryService implements UnlinkMyVeterinaryUseCase {

    private final UnlinkUseCase unlinkUseCase;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    public UnlinkResponse unlinkMine(UUID veterinaryId) {
        return unlinkUseCase.unlink(authenticatedUserPort.current().userId(), veterinaryId);
    }
}
