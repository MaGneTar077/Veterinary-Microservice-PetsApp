package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RejectVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.RejectVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RejectVerificationUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.CannotReviewOwnRequestException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidReviewDecisionException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RejectVerificationService implements RejectVerificationUseCase {

    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_NOTES_LENGTH = 1000;

    private final VeterinaryAuthorizationService authorizationService;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public RejectVerificationResponse reject(UUID requestId, RejectVerificationRequest request) {
        authorizationService.requirePlatformAdmin();

        if (request.getReasonCode() == null) {
            throw new InvalidReviewDecisionException("reasonCode is required");
        }
        if (request.getNotes() == null || request.getNotes().isBlank()) {
            throw new InvalidReviewDecisionException("notes is required");
        }
        if (request.getNotes().length() > MAX_NOTES_LENGTH) {
            throw new InvalidReviewDecisionException("notes must be " + MAX_NOTES_LENGTH + " characters or fewer");
        }

        VerificationRequest existing = verificationRequestRepositoryPort.findById(requestId)
                .orElseThrow(VerificationRequestNotFoundException::new);
        if (existing.getStatus() != VerificationRequestStatus.UNDER_REVIEW) {
            throw new InvalidVerificationStateException();
        }

        AuthenticatedUser caller = authenticatedUserPort.current();
        if (existing.getSubmittedBy().equals(caller.userId())) {
            throw new CannotReviewOwnRequestException();
        }

        boolean claimed = verificationRequestRepositoryPort.claimForDecision(
                requestId, VerificationRequestStatus.UNDER_REVIEW, VerificationRequestStatus.REJECTED);
        if (!claimed) {
            throw new InvalidVerificationStateException();
        }

        LocalDateTime now = LocalDateTime.now();
        VerificationRequest decided = existing.toBuilder()
                .status(VerificationRequestStatus.REJECTED)
                .reviewerId(caller.userId())
                .reviewerNotes(request.getNotes())
                .rejectionReasonCode(request.getReasonCode())
                .decidedAt(now)
                .build();
        VerificationRequest saved = verificationRequestRepositoryPort.save(decided);

        if (existing.getSubjectType() == SubjectType.CLINIC) {
            rejectClinic(existing.getSubjectId());
        } else {
            rejectProfessional(existing.getSubjectId(), request.getNotes(), now);
        }

        int attemptsRemaining = MAX_ATTEMPTS - existing.getAttemptNumber();
        LocalDateTime documentsDeletionDate = now.plusDays(90);

        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.VERIFICATION_REJECTED,
                existing.getSubmittedBy(),
                null,
                Map.of("subjectType", existing.getSubjectType().name(),
                        "reasonCode", request.getReasonCode().name(),
                        "notes", request.getNotes(),
                        "attemptsRemaining", attemptsRemaining,
                        "documentsDeletionDate", documentsDeletionDate),
                EventMetadata.of(existing.getSubjectType() == SubjectType.CLINIC ? existing.getSubjectId() : null, null)));

        return RejectVerificationResponse.builder()
                .requestId(saved.getId())
                .subjectType(saved.getSubjectType())
                .status(saved.getStatus())
                .decidedAt(saved.getDecidedAt())
                .attemptsRemaining(attemptsRemaining)
                .documentsDeletionDate(documentsDeletionDate)
                .build();
    }

    private void rejectClinic(UUID veterinaryId) {
        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);
        veterinaryRepositoryPort.save(veterinary.toBuilder()
                .status(VeterinaryStatus.REJECTED)
                .active(VeterinaryStatus.REJECTED.impliesActiveFlag())
                .build());
    }

    private void rejectProfessional(UUID profileId, String reason, LocalDateTime now) {
        ProfessionalProfile profile = professionalProfileRepositoryPort.findById(profileId)
                .orElseThrow(ProfessionalProfileNotFoundException::new);
        professionalProfileRepositoryPort.save(profile.toBuilder()
                .licenseStatus(LicenseStatus.REJECTED)
                .rejectionReason(reason)
                .updatedAt(now)
                .build());
    }
}
