package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnlinkService implements UnlinkUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public UnlinkResponse unlink(UUID userId, UUID veterinaryId) {
        authorizationService.requireSelfOrPermission(userId, veterinaryId, Permission.CLINIC_CONFIGURE);

        veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        UserVeterinaryLink link = linkRepositoryPort
                .findByUserIdAndVeterinaryId(userId, veterinaryId)
                .orElseThrow(UserNotLinkedException::new);

        linkRepositoryPort.deleteById(link.getId());

        return UnlinkResponse.builder()
                .userId(userId)
                .veterinaryId(veterinaryId)
                .message("User unlinked successfully")
                .build();
    }
}
