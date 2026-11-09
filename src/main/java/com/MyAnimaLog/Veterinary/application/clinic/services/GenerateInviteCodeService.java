package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GenerateInviteCodeRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.GenerateInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GenerateInviteCodeUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.services.LinkCodes;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Genera (o regenera) el código con el que los dueños de mascotas se vinculan a la clínica.
 *
 * <p>Regenerar <strong>invalida el código anterior</strong>: el que ya estaba impreso o compartido
 * deja de resolver. Es intencional — es la única forma que tiene una clínica de cortar un código que
 * se filtró.
 *
 * <p>Exige la clínica {@code ACTIVE} desde la Fase 4A: un código de una clínica que todavía no fue
 * aprobada no sirve para nada (vincularse lo rechaza igual, ver {@code LinkCodes}), así que
 * entregarlo solo generaría un reclamo de "no funciona". {@code requireActiveMember} —por donde pasa
 * {@code require}— solo descarta {@code SUSPENDED}, y {@code CLINIC_CONFIGURE} sobrevive en
 * {@code PENDING_DOCUMENTS}, así que este chequeo hace falta aparte.
 */
@Service
@RequiredArgsConstructor
public class GenerateInviteCodeService implements GenerateInviteCodeUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Value("${app.link.base-url}")
    private String linkBaseUrl;

    @Override
    public GenerateInviteCodeResponse generateInviteCode(GenerateInviteCodeRequest request) {
        authorizationService.require(request.getVeterinaryId(), Permission.CLINIC_CONFIGURE);

        Veterinary veterinary = veterinaryRepositoryPort.findById(request.getVeterinaryId())
                .orElseThrow(VeterinaryNotFoundException::new);

        if (veterinary.getStatus() != VeterinaryStatus.ACTIVE) {
            throw new ClinicNotActiveException();
        }

        String inviteCode = LinkCodes.generateUnique(veterinaryRepositoryPort);

        Veterinary saved = veterinaryRepositoryPort.save(veterinary.toBuilder()
                .inviteCode(inviteCode)
                .inviteLink(LinkCodes.buildLink(linkBaseUrl, inviteCode))
                .build());

        return GenerateInviteCodeResponse.builder()
                .id(saved.getId())
                .inviteCode(saved.getInviteCode())
                .inviteLink(saved.getInviteLink())
                .build();
    }
}
