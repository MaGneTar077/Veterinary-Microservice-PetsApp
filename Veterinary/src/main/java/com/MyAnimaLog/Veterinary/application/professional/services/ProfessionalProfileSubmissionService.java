package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.SubmitProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.dto.Recipient;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.ProfessionalDocumentType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.MissingRequiredDocumentsException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The actual DRAFT/NEEDS_CORRECTION -> UNDER_REVIEW transition for a professional profile,
 * shared by the standalone {@code POST /me/professional-profile/submit} endpoint and
 * {@code SubmitClinicVerification}'s {@code includeProfessionalProfile=true} path — both must
 * apply exactly the same validations, and the clinic path needs them to run inside its own
 * transaction (no {@code @Transactional} here; it joins whichever transaction the caller is in).
 */
@Service
@RequiredArgsConstructor
public class ProfessionalProfileSubmissionService {

    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    @Value("${app.support-email}")
    private String supportEmail;

    public SubmitProfessionalProfileResponse submitForUser(UUID userId) {
        ProfessionalProfile profile = professionalProfileRepositoryPort.findByUserId(userId)
                .orElseThrow(ProfessionalProfileNotFoundException::new);

        VerificationRequest latest = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId())
                .orElseThrow(VerificationRequestNotFoundException::new);
        if (latest.getStatus() != VerificationRequestStatus.DRAFT
                && latest.getStatus() != VerificationRequestStatus.NEEDS_CORRECTION) {
            throw new InvalidVerificationStateException();
        }

        List<String> missing = missingDocuments(latest.getId());
        if (!missing.isEmpty()) {
            throw new MissingRequiredDocumentsException("Missing required: " + String.join(", ", missing));
        }

        LocalDateTime now = LocalDateTime.now();
        VerificationRequest savedRequest = verificationRequestRepositoryPort.save(latest.toBuilder()
                .status(VerificationRequestStatus.UNDER_REVIEW)
                .submittedAt(now)
                .correctionDocumentTypes(null)
                .correctionRequestedAt(null)
                .build());

        ProfessionalProfile savedProfile = professionalProfileRepositoryPort.save(profile.toBuilder()
                .licenseStatus(LicenseStatus.PENDING)
                .updatedAt(now)
                .build());

        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.VERIFICATION_SUBMITTED,
                null,
                new Recipient(supportEmail, null, null),
                Map.of("subjectType", "PROFESSIONAL", "profileId", savedProfile.getId(), "requestId", savedRequest.getId()),
                EventMetadata.of(null, null)));

        return SubmitProfessionalProfileResponse.builder()
                .profileId(savedProfile.getId())
                .requestId(savedRequest.getId())
                .status(savedRequest.getStatus())
                .licenseStatus(savedProfile.getLicenseStatus())
                .submittedAt(savedRequest.getSubmittedAt())
                .build();
    }

    private List<String> missingDocuments(UUID requestId) {
        Set<String> present = verificationDocumentRepositoryPort.findByRequestId(requestId).stream()
                .map(VerificationDocument::getDocumentType)
                .collect(Collectors.toSet());
        List<String> missing = new ArrayList<>();
        for (ProfessionalDocumentType type : ProfessionalDocumentType.values()) {
            if (type.isRequired() && !present.contains(type.name())) {
                missing.add(type.name());
            }
        }
        return missing;
    }
}
