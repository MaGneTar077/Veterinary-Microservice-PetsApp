package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.GenerateInviteCodeRequest;
import com.MyAnimaLog.Veterinary.application.dto.GenerateInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.GenerateInviteCodeUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class GenerateInviteCodeService implements GenerateInviteCodeUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public GenerateInviteCodeResponse generateInviteCode(GenerateInviteCodeRequest request) {
        Veterinary veterinary = veterinaryRepositoryPort.findById(request.getVeterinaryId())
                .orElseThrow(VeterinaryNotFoundException::new);

        String inviteCode = generateUniqueCode();
        String inviteLink = baseUrl + "/api/veterinary/join?code=" + inviteCode;

        Veterinary updated = veterinary.toBuilder()
                .inviteCode(inviteCode)
                .inviteLink(inviteLink)
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(updated);

        return GenerateInviteCodeResponse.builder()
                .id(saved.getId())
                .inviteCode(saved.getInviteCode())
                .inviteLink(saved.getInviteLink())
                .build();
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = "VET-" + randomString(8);
        } while (veterinaryRepositoryPort.existsByInviteCode(code));
        return code;
    }

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
