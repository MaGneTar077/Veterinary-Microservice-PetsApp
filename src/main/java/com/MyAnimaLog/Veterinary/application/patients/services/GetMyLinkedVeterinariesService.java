package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.GetMyLinkedVeterinariesResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetMyLinkedVeterinariesUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Las clínicas con las que el dueño está vinculado hoy, con cuántas mascotas le comparte a cada una.
 * Token normal y sin ningún parámetro de usuario: es siempre el del token.
 *
 * <p>Los nombres de las clínicas se resuelven en <strong>un solo</strong>
 * {@code findByIdIn} en vez de un {@code findById} por fila (mismo criterio que los listados del
 * admin). El conteo de mascotas sí es una consulta por clínica, pero es un {@code COUNT} sobre el
 * índice {@code ix_patients_owner} y el listado está acotado por naturaleza: son las veterinarias de
 * una persona, no una página de un catálogo.
 */
@Service
@RequiredArgsConstructor
public class GetMyLinkedVeterinariesService implements GetMyLinkedVeterinariesUseCase {

    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryPatientRepositoryPort patientRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    public GetMyLinkedVeterinariesResponse list() {
        UUID userId = authenticatedUserPort.current().userId();

        var links = linkRepositoryPort.findActiveByUserId(userId);
        Map<UUID, Veterinary> clinics = veterinaryRepositoryPort
                .findByIdIn(links.stream().map(UserVeterinaryLink::getVeterinaryId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Veterinary::getId, Function.identity()));

        return GetMyLinkedVeterinariesResponse.builder()
                .veterinaries(links.stream()
                        .map(link -> toDto(link, clinics.get(link.getVeterinaryId()), userId))
                        // Una clínica borrada de la base dejaría la fila sin clínica que mostrar.
                        // No debería pasar (la FK es ON DELETE CASCADE), pero devolver una entrada
                        // a medias sería peor que omitirla.
                        .filter(java.util.Objects::nonNull)
                        .toList())
                .build();
    }

    private GetMyLinkedVeterinariesResponse.LinkedVeterinary toDto(UserVeterinaryLink link,
                                                                    Veterinary veterinary,
                                                                    UUID userId) {
        if (veterinary == null) {
            return null;
        }
        return GetMyLinkedVeterinariesResponse.LinkedVeterinary.builder()
                .veterinaryId(veterinary.getId())
                .name(veterinary.getName())
                .logoUrl(veterinary.getLogoUrl())
                .city(veterinary.getCity())
                .phone(veterinary.getPhone())
                .status(veterinary.getStatus())
                .linkedAt(link.getLinkedAt())
                .sharedPetsCount(patientRepositoryPort
                        .countGrantedByVeterinaryIdAndGrantedBy(veterinary.getId(), userId))
                .build();
    }
}
