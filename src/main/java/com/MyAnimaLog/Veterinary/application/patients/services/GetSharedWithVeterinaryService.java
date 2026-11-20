package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetSharedWithVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetSharedWithVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Endpoint interno para Calendar (CALENDAR_REQUESTS_TO_VETERINARY.md, pedido 5): que el dueño solo
 * reserve con clínicas que tienen compartida su mascota, sin depender de que exista un empleado
 * activo concreto (a diferencia del atajo provisional que usaba {@code access}).
 *
 * <p>{@code shared} exige dos cosas a la vez: consentimiento {@code GRANTED} de esa mascota con esa
 * clínica, <strong>y</strong> que el dueño que lo dio ({@code grantedBy}) siga vinculado
 * ({@code LINKED}) a esa clínica — si se desvinculó, {@code UnlinkService} ya revocó el
 * consentimiento en la misma transacción, pero esta doble verificación no depende de ese orden.
 *
 * <p><strong>Nunca responde 404</strong>, igual que {@code access}: clínica inexistente, mascota
 * inexistente o consentimiento revocado son todos {@code 200} con {@code shared=false}. El estado
 * de la clínica ({@code ACTIVE}/{@code SUSPENDED}/...) no se consulta aquí — Calendar lo pide por
 * separado (pedido 1). No expone el id del dueño.
 */
@Service
@RequiredArgsConstructor
public class GetSharedWithVeterinaryService implements GetSharedWithVeterinaryUseCase {

    private final VeterinaryPatientRepositoryPort patientRepositoryPort;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Override
    public GetSharedWithVeterinaryResponse get(UUID veterinaryId, UUID petId) {
        boolean shared = patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId)
                .filter(VeterinaryPatient::isGranted)
                .map(VeterinaryPatient::getGrantedBy)
                .filter(Objects::nonNull)
                .flatMap(ownerId -> linkRepositoryPort.findActiveLink(ownerId, veterinaryId))
                .isPresent();

        return GetSharedWithVeterinaryResponse.builder().shared(shared).build();
    }
}
