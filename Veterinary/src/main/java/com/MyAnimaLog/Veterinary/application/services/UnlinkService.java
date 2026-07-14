package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UnlinkUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.UserVeterinaryLink;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnlinkService implements UnlinkUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Override
    public UnlinkResponse unlink(UUID userId, UUID veterinaryId) {

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
