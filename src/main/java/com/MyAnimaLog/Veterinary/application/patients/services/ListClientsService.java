package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListClientsResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.ListClientsUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Dueños vinculados a la clínica, paginado. {@code CLINICAL_READ_BASIC}: lo tienen los 5 roles
 * (§3.2), es el más bajo que ya exige el plan §7.1 para esta pantalla.
 *
 * <p>Nombre/correo prefieren el dato <strong>vivo</strong> de {@code UserServicePort} (una
 * resolución en lote para toda la página) y caen al snapshot del link solo si User no responde —
 * best-effort, nunca rompe el listado.
 */
@Service
@RequiredArgsConstructor
public class ListClientsService implements ListClientsUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final VeterinaryAuthorizationService authorizationService;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;
    private final VeterinaryPatientRepositoryPort patientRepositoryPort;
    private final UserServicePort userServicePort;

    @Override
    public ListClientsResponse list(UUID veterinaryId, Integer page, Integer size) {
        authorizationService.require(veterinaryId, Permission.CLINICAL_READ_BASIC);

        int effectivePage = page != null ? page : 0;
        int effectiveSize = size != null ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;

        UserVeterinaryLinkRepositoryPort.ClientsPage result =
                linkRepositoryPort.findActiveByVeterinaryId(veterinaryId, effectivePage, effectiveSize);

        Map<UUID, UserServicePort.UserInfo> users = userServicePort.getUsers(
                result.items().stream().map(UserVeterinaryLink::getUserId).distinct().toList());

        return ListClientsResponse.builder()
                .items(result.items().stream().map(link -> toDto(link, users.get(link.getUserId()),
                        patientRepositoryPort.countGrantedByVeterinaryIdAndGrantedBy(
                                veterinaryId, link.getUserId()))).toList())
                .page(effectivePage)
                .size(effectiveSize)
                .total(result.total())
                .build();
    }

    private ListClientsResponse.Client toDto(UserVeterinaryLink link, UserServicePort.UserInfo user,
                                              long sharedPetsCount) {
        return ListClientsResponse.Client.builder()
                .userId(link.getUserId())
                .name(user != null && user.name() != null ? user.name() : link.getOwnerName())
                .email(user != null && user.email() != null ? user.email() : link.getOwnerEmail())
                .linkedAt(link.getLinkedAt())
                .sharedPetsCount(sharedPetsCount)
                .build();
    }
}
