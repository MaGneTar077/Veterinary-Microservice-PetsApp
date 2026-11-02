package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RequestVerificationCorrectionRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.RequestVerificationCorrectionResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RequestVerificationCorrectionUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.ClinicDocumentType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.ProfessionalDocumentType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.CannotReviewOwnRequestException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidDocumentTypeException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidReviewDecisionException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestVerificationCorrectionService implements RequestVerificationCorrectionUseCase {

    private static final int MAX_NOTES_LENGTH = 1000;
    private static final Set<String> CLINIC_DOCUMENT_TYPES = Arrays.stream(ClinicDocumentType.values())
            .map(Enum::name).collect(Collectors.toSet());
    private static final Set<String> PROFESSIONAL_DOCUMENT_TYPES = Arrays.stream(ProfessionalDocumentType.values())
            .map(Enum::name).collect(Collectors.toSet());

    private final VeterinaryAuthorizationService authorizationService;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public RequestVerificationCorrectionResponse requestCorrection(UUID requestId, RequestVerificationCorrectionRequest request) {
        authorizationService.requirePlatformAdmin();

        if (request.getNotes() == null || request.getNotes().isBlank()) {
            throw new InvalidReviewDecisionException("notes is required");
        }
        if (request.getNotes().length() > MAX_NOTES_LENGTH) {
            throw new InvalidReviewDecisionException("notes must be " + MAX_NOTES_LENGTH + " characters or fewer");
        }
        if (request.getDocumentTypes() == null || request.getDocumentTypes().isEmpty()) {
            throw new InvalidReviewDecisionException("documentTypes is required");
        }

        VerificationRequest existing = verificationRequestRepositoryPort.findById(requestId)
                .orElseThrow(VerificationRequestNotFoundException::new);
        if (existing.getStatus() != VerificationRequestStatus.UNDER_REVIEW) {
            throw new InvalidVerificationStateException();
        }

        Set<String> validTypes = existing.getSubjectType() == SubjectType.CLINIC ? CLINIC_DOCUMENT_TYPES : PROFESSIONAL_DOCUMENT_TYPES;
        for (String type : request.getDocumentTypes()) {
            if (!validTypes.contains(type)) {
                throw new InvalidDocumentTypeException("documentTypes contains an invalid type for this subject: " + type);
            }
        }

        AuthenticatedUser caller = authenticatedUserPort.current();
        if (existing.getSubmittedBy().equals(caller.userId())) {
            throw new CannotReviewOwnRequestException();
        }

        boolean claimed = verificationRequestRepositoryPort.claimForDecision(
                requestId, VerificationRequestStatus.UNDER_REVIEW, VerificationRequestStatus.NEEDS_CORRECTION);
        if (!claimed) {
            throw new InvalidVerificationStateException();
        }

        LocalDateTime now = LocalDateTime.now();
        VerificationRequest updated = existing.toBuilder()
                .status(VerificationRequestStatus.NEEDS_CORRECTION)
                .reviewerId(caller.userId())
                .reviewerNotes(request.getNotes())
                .correctionDocumentTypes(request.getDocumentTypes())
                .correctionRequestedAt(now)
                .build();
        VerificationRequest saved = verificationRequestRepositoryPort.save(updated);

        if (existing.getSubjectType() == SubjectType.CLINIC) {
            requestClinicCorrection(existing.getSubjectId());
        } else {
            requestProfessionalCorrection(existing.getSubjectId(), now);
        }

        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.VERIFICATION_CORRECTION_REQUESTED,
                existing.getSubmittedBy(),
                null,
                Map.of("subjectType", existing.getSubjectType().name(),
                        "notes", request.getNotes(),
                        "documentTypes", request.getDocumentTypes()),
                EventMetadata.of(existing.getSubjectType() == SubjectType.CLINIC ? existing.getSubjectId() : null, null)));

        return RequestVerificationCorrectionResponse.builder()
                .requestId(saved.getId())
                .subjectType(saved.getSubjectType())
                .status(saved.getStatus())
                .correctionDocumentTypes(saved.getCorrectionDocumentTypes())
                .correctionRequestedAt(saved.getCorrectionRequestedAt())
                .build();
    }

    private void requestClinicCorrection(UUID veterinaryId) {
        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);
        veterinaryRepositoryPort.save(veterinary.toBuilder()
                .status(VeterinaryStatus.NEEDS_CORRECTION)
                .active(VeterinaryStatus.NEEDS_CORRECTION.impliesActiveFlag())
                .build());
    }

    private void requestProfessionalCorrection(UUID profileId, LocalDateTime now) {
        ProfessionalProfile profile = professionalProfileRepositoryPort.findById(profileId)
                .orElseThrow(ProfessionalProfileNotFoundException::new);
        professionalProfileRepositoryPort.save(profile.toBuilder()
                .licenseStatus(LicenseStatus.NOT_SUBMITTED)
                .updatedAt(now)
                .build());
    }
}
