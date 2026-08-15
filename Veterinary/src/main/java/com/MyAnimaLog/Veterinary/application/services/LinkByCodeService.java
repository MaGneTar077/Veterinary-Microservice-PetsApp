package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.LinkByCodeUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.exceptions.UserAlreadyLinkedException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.domain.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkByCodeService implements LinkByCodeUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Override
    public LinkByCodeResponse linkByCode(LinkByCodeRequest request) {

        if (request.getInviteCode() == null || request.getInviteCode().isBlank()) {
            throw new InvalidInviteCodeException();
        }

        Veterinary veterinary = veterinaryRepositoryPort
                .findByInviteCode(request.getInviteCode())
                .orElseThrow(InvalidInviteCodeException::new);

        if (!veterinary.getActive()) {
            throw new VeterinaryNotActiveException();
        }

        if (linkRepositoryPort.existsByUserIdAndVeterinaryId(
                request.getUserId(), veterinary.getId())) {
            throw new UserAlreadyLinkedException();
        }

        UserVeterinaryLink link = UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .veterinaryId(veterinary.getId())
                .status("LINKED")
                .linkedAt(LocalDateTime.now())
                .build();

        UserVeterinaryLink saved = linkRepositoryPort.save(link);

        return LinkByCodeResponse.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .veterinaryId(saved.getVeterinaryId())
                .status(saved.getStatus())
                .linkedAt(saved.getLinkedAt())
                .build();
    }
}
