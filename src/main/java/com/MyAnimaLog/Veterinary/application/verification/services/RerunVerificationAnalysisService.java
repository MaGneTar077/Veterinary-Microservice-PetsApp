package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RerunVerificationAnalysisResponse;
import com.MyAnimaLog.Veterinary.application.verification.dto.VerificationSubmittedEvent;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RerunVerificationAnalysisUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.IaVerificationPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.AiVerificationDisabledException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RerunVerificationAnalysisService implements RerunVerificationAnalysisUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final IaVerificationPort iaVerificationPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public RerunVerificationAnalysisResponse rerun(UUID requestId) {
        authorizationService.requirePlatformAdmin();

        if (!iaVerificationPort.isEnabled()) {
            throw new AiVerificationDisabledException();
        }

        VerificationRequest request = verificationRequestRepositoryPort.findById(requestId)
                .orElseThrow(VerificationRequestNotFoundException::new);

        applicationEventPublisher.publishEvent(new VerificationSubmittedEvent(request.getId()));

        return RerunVerificationAnalysisResponse.builder()
                .requestId(request.getId())
                .aiStatus(request.getAiStatus())
                .build();
    }
}
