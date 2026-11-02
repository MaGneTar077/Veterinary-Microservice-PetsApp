package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.DeleteInviteCodeUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteInviteCodeService implements DeleteInviteCodeUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public DeleteInviteCodeResponse deleteInviteCode(UUID veterinaryId) {
        authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        Veterinary updated = veterinary.toBuilder()
                .inviteCode(null)
                .inviteLink(null)
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(updated);

        return DeleteInviteCodeResponse.builder()
                .id(saved.getId())
                .inviteCode(saved.getInviteCode())
                .inviteLink(saved.getInviteLink())
                .build();
    }
}
