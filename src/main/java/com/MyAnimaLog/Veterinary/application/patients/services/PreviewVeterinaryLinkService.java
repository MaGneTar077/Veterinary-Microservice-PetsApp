package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.PreviewVeterinaryLinkUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Qué clínica hay detrás de un código, para que el dueño confirme antes de vincularse. No escribe
 * nada: es solo lectura, y sigue siendo así incluso si el dueño ya está vinculado.
 *
 * <p>Pide token normal (como todo el flujo del dueño) aunque el dato que devuelve no sea sensible:
 * así esto no queda como un endpoint público donde se puedan probar códigos en masa sin dejar
 * rastro de quién.
 */
@Service
@RequiredArgsConstructor
public class PreviewVeterinaryLinkService implements PreviewVeterinaryLinkUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    public PreviewVeterinaryLinkResponse preview(PreviewVeterinaryLinkRequest request) {
        UUID userId = authenticatedUserPort.current().userId();
        Veterinary veterinary = LinkCodes.resolveActiveClinic(veterinaryRepositoryPort, request.getCode());

        return PreviewVeterinaryLinkResponse.builder()
                .veterinaryId(veterinary.getId())
                .name(veterinary.getName())
                .logoUrl(veterinary.getLogoUrl())
                .city(veterinary.getCity())
                .department(veterinary.getDepartment())
                .address(veterinary.getAddress())
                .alreadyLinked(linkRepositoryPort.findActiveLink(userId, veterinary.getId()).isPresent())
                .build();
    }
}
