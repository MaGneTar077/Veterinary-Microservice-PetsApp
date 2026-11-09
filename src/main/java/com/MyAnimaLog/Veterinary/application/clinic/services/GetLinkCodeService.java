package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetLinkCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetLinkCodeUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Ver el código de vinculación sin regenerarlo. Existe separado de
 * {@code GenerateInviteCodeService} porque regenerar <strong>invalida el código anterior</strong>:
 * si la pantalla tuviera que llamar a generar para mostrarlo, cada vez que alguien la abre rompería
 * los códigos que la clínica ya repartió.
 *
 * <p>A diferencia de generar, esto <strong>no</strong> exige la clínica {@code ACTIVE}: mostrarle a
 * la clínica el código que ya tiene no causa ningún daño, y esconderlo mientras está suspendida solo
 * la confundiría. (De todos modos vincularse con él sí se rechaza.)
 */
@Service
@RequiredArgsConstructor
public class GetLinkCodeService implements GetLinkCodeUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public GetLinkCodeResponse getLinkCode(UUID veterinaryId) {
        authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        return GetLinkCodeResponse.builder()
                .veterinaryId(veterinary.getId())
                .inviteCode(veterinary.getInviteCode())
                .inviteLink(veterinary.getInviteLink())
                .hasCode(veterinary.getInviteCode() != null && !veterinary.getInviteCode().isBlank())
                .build();
    }
}
