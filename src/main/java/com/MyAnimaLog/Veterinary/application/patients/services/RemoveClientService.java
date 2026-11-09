package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.RemoveClientUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * "La clínica desvincula a un cliente", lado opuesto de {@code UnlinkMyVeterinaryService}.
 *
 * <p>Delega en {@link UnlinkUseCase}, que ya exige {@code CLINIC_CONFIGURE} cuando el caller no es
 * el propio usuario ({@code requireSelfOrPermission}) y hace la misma cascada (revoca todas las
 * mascotas compartidas, marca {@code UNLINKED}, borra correo/teléfono del snapshot). No hay una
 * segunda implementación que pueda divergir de esas reglas.
 */
@Service
@RequiredArgsConstructor
public class RemoveClientService implements RemoveClientUseCase {

    private final UnlinkUseCase unlinkUseCase;

    @Override
    public UnlinkResponse remove(UUID veterinaryId, UUID userId) {
        return unlinkUseCase.unlink(userId, veterinaryId);
    }
}
