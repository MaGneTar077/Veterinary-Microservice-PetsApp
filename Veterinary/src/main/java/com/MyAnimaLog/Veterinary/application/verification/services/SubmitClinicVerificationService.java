package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.services.ProfessionalProfileSubmissionService;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.dto.Recipient;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.SubmitClinicVerificationUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.ClinicDocumentType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.MissingRequiredDocumentsException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.TruthfulnessDeclarationRequiredException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmitClinicVerificationService implements SubmitClinicVerificationUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final EventPublisherPort eventPublisherPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final ProfessionalProfileSubmissionService professionalProfileSubmissionService;

    @Value("${app.support-email}")
    private String supportEmail;

    @Override
    @Transactional
    public SubmitClinicVerificationResponse submit(UUID veterinaryId, SubmitClinicVerificationRequest request) {
        authorizationService.requireVeterinaryOwner(veterinaryId);

        if (!Boolean.TRUE.equals(request.getTruthfulnessDeclared())) {
            throw new TruthfulnessDeclarationRequiredException();
        }

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        VerificationRequest latest = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId)
                .orElseThrow(VerificationRequestNotFoundException::new);
        if (latest.getStatus() != VerificationRequestStatus.DRAFT
                && latest.getStatus() != VerificationRequestStatus.NEEDS_CORRECTION) {
            throw new InvalidVerificationStateException();
        }

        List<String> missing = new ArrayList<>();
        if (!isLegalRepComplete(veterinary)) {
            missing.add("legalRepresentative");
        }
        Map<String, VerificationDocument> documentsByType = verificationDocumentRepositoryPort.findByRequestId(latest.getId())
                .stream()
                .collect(Collectors.toMap(VerificationDocument::getDocumentType, d -> d));
        for (ClinicDocumentType type : ClinicDocumentType.values()) {
            if (type.isRequired() && !documentsByType.containsKey(type.name())) {
                missing.add(type.name());
            }
        }
        if (!missing.isEmpty()) {
            throw new MissingRequiredDocumentsException("Missing required: " + String.join(", ", missing));
        }

        LocalDateTime now = LocalDateTime.now();
        VerificationRequest updatedRequest = latest.toBuilder()
                .status(VerificationRequestStatus.UNDER_REVIEW)
                .submittedAt(now)
                .truthfulnessDeclaredAt(now)
                .correctionDocumentTypes(null)
                .correctionRequestedAt(null)
                .build();
        VerificationRequest savedRequest = verificationRequestRepositoryPort.save(updatedRequest);

        if (Boolean.TRUE.equals(request.getIncludeProfessionalProfile())) {
            AuthenticatedUser caller = authenticatedUserPort.current();
            professionalProfileSubmissionService.submitForUser(caller.userId());
        }

        veterinaryRepositoryPort.save(veterinary.toBuilder()
                .status(VeterinaryStatus.UNDER_REVIEW)
                .active(VeterinaryStatus.UNDER_REVIEW.impliesActiveFlag())
                .build());

        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.VERIFICATION_SUBMITTED,
                null,
                new Recipient(supportEmail, null, null),
                Map.of("veterinaryId", veterinaryId, "requestId", savedRequest.getId(), "attemptNumber", savedRequest.getAttemptNumber()),
                EventMetadata.of(veterinaryId, null)));

        return SubmitClinicVerificationResponse.builder()
                .veterinaryId(veterinaryId)
                .requestId(savedRequest.getId())
                .status(savedRequest.getStatus())
                .submittedAt(savedRequest.getSubmittedAt())
                .build();
    }

    private boolean isLegalRepComplete(Veterinary veterinary) {
        return veterinary.getLegalRepIsOwner() != null
                && veterinary.getLegalRepName() != null && !veterinary.getLegalRepName().isBlank()
                && veterinary.getLegalRepDocumentType() != null && !veterinary.getLegalRepDocumentType().isBlank()
                && veterinary.getLegalRepDocumentNumber() != null && !veterinary.getLegalRepDocumentNumber().isBlank();
    }
}
