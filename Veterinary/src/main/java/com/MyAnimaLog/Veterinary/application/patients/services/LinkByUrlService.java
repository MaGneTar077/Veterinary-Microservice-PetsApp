package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkByUrlUseCase;
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
public class LinkByUrlService implements LinkByUrlUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;


    @Override
    public LinkByUrlResponse linkByUrl(LinkByUrlRequest request) {

        if (request.getInviteLink() == null || request.getInviteLink().isBlank()) {
            throw new InvalidInviteCodeException("Invite link is invalid or does not exist");
        }

        Veterinary veterinary = veterinaryRepositoryPort
                .findByInviteLink(request.getInviteLink())
                .orElseThrow(() -> new InvalidInviteCodeException("Invite link is invalid or does not exist"));

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

        return LinkByUrlResponse.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .veterinaryId(saved.getVeterinaryId())
                .status(saved.getStatus())
                .linkedAt(saved.getLinkedAt())
                .build();
    }
}
