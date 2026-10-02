package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkByCodeUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserAlreadyLinkedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
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
