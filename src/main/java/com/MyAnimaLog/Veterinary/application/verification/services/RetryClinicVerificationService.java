package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RetryClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RetryClinicVerificationUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationAttemptsExhaustedException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RetryClinicVerificationService implements RetryClinicVerificationUseCase {

    private static final int MAX_ATTEMPTS = 3;

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    @Transactional
    public RetryClinicVerificationResponse retry(UUID veterinaryId) {
        authorizationService.requireVeterinaryOwner(veterinaryId);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        VerificationRequest latest = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId)
                .orElseThrow(VerificationRequestNotFoundException::new);
        if (latest.getStatus() != VerificationRequestStatus.REJECTED) {
            throw new InvalidVerificationStateException("Only a REJECTED request can be retried");
        }
        if (latest.getAttemptNumber() >= MAX_ATTEMPTS) {
            throw new VerificationAttemptsExhaustedException();
        }

        AuthenticatedUser caller = authenticatedUserPort.current();
        VerificationRequest newRequest = VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(SubjectType.CLINIC)
                .subjectId(veterinaryId)
                .status(VerificationRequestStatus.DRAFT)
                .attemptNumber(latest.getAttemptNumber() + 1)
                .submittedBy(caller.userId())
                .registryChecked(false)
                .createdAt(LocalDateTime.now())
                .build();
        VerificationRequest saved = verificationRequestRepositoryPort.save(newRequest);

        veterinaryRepositoryPort.save(veterinary.toBuilder()
                .status(VeterinaryStatus.PENDING_DOCUMENTS)
                .active(VeterinaryStatus.PENDING_DOCUMENTS.impliesActiveFlag())
                .build());

        return RetryClinicVerificationResponse.builder()
                .veterinaryId(veterinaryId)
                .requestId(saved.getId())
                .status(saved.getStatus())
                .attemptNumber(saved.getAttemptNumber())
                .build();
    }
}
