package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.dto.ApproveVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.ApproveVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.ApproveVerificationUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.CannotReviewOwnRequestException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.RegistryCheckRequiredException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApproveVerificationService implements ApproveVerificationUseCase {

    private static final int TRIAL_DAYS = 30;

    private final VeterinaryAuthorizationService authorizationService;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public ApproveVerificationResponse approve(UUID requestId, ApproveVerificationRequest request) {
        authorizationService.requirePlatformAdmin();

        if (!Boolean.TRUE.equals(request.getRegistryChecked())) {
            throw new RegistryCheckRequiredException();
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
                requestId, VerificationRequestStatus.UNDER_REVIEW, VerificationRequestStatus.APPROVED);
        if (!claimed) {
            throw new InvalidVerificationStateException();
        }

        LocalDateTime now = LocalDateTime.now();
        VerificationRequest decided = existing.toBuilder()
                .status(VerificationRequestStatus.APPROVED)
                .reviewerId(caller.userId())
                .reviewerNotes(request.getNotes())
                .registryChecked(true)
                .decidedAt(now)
                .build();
        VerificationRequest saved = verificationRequestRepositoryPort.save(decided);

        LocalDate trialEndsAt = null;
        String subjectName;
        if (existing.getSubjectType() == SubjectType.CLINIC) {
            trialEndsAt = approveClinic(existing.getSubjectId(), existing.getSubmittedBy(), now);
            subjectName = veterinaryRepositoryPort.findById(existing.getSubjectId())
                    .map(Veterinary::getName).orElse(null);
        } else {
            subjectName = approveProfessional(existing.getSubjectId(), caller.userId(), now);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("subjectType", existing.getSubjectType().name());
        payload.put("name", subjectName);
        if (trialEndsAt != null) {
            payload.put("trialEndsAt", trialEndsAt);
        }
        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.VERIFICATION_APPROVED,
                existing.getSubmittedBy(),
                null,
                payload,
                EventMetadata.of(existing.getSubjectType() == SubjectType.CLINIC ? existing.getSubjectId() : null, null)));

        return ApproveVerificationResponse.builder()
                .requestId(saved.getId())
                .subjectType(saved.getSubjectType())
                .status(saved.getStatus())
                .decidedAt(saved.getDecidedAt())
                .trialEndsAt(trialEndsAt)
                .build();
    }

    private LocalDate approveClinic(UUID veterinaryId, UUID submittedBy, LocalDateTime now) {
        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);
        veterinaryRepositoryPort.save(veterinary.toBuilder()
                .status(VeterinaryStatus.ACTIVE)
                .active(VeterinaryStatus.ACTIVE.impliesActiveFlag())
                .approvedAt(now)
                .build());

        if (subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)) {
            return null;
        }
        LocalDate startDate = now.toLocalDate();
        LocalDate endDate = startDate.plusDays(TRIAL_DAYS);
        subscriptionRepositoryPort.save(VeterinarySubscription.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .plan("PRO_TRIAL")
                .startDate(startDate)
                .endDate(endDate)
                .active(true)
                .build());
        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.SUBSCRIPTION_TRIAL_STARTED,
                submittedBy,
                null,
                Map.of("veterinaryId", veterinaryId, "trialEndsAt", endDate),
                EventMetadata.of(veterinaryId, null)));
        return endDate;
    }

    private String approveProfessional(UUID profileId, UUID verifiedBy, LocalDateTime now) {
        ProfessionalProfile profile = professionalProfileRepositoryPort.findById(profileId)
                .orElseThrow(ProfessionalProfileNotFoundException::new);
        ProfessionalProfile saved = professionalProfileRepositoryPort.save(profile.toBuilder()
                .licenseStatus(LicenseStatus.VERIFIED)
                .verifiedAt(now)
                .verifiedBy(verifiedBy)
                .rejectionReason(null)
                .updatedAt(now)
                .build());
        return saved.getFullName();
    }
}
