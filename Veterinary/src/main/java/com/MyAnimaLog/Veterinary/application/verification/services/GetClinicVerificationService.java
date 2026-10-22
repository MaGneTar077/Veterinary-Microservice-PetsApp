package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.GetClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.dto.LegalRepresentativeInfo;
import com.MyAnimaLog.Veterinary.application.verification.dto.ProfessionalProfileSummary;
import com.MyAnimaLog.Veterinary.application.verification.dto.VerificationDocumentStatusInfo;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.GetClinicVerificationUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.ClinicDocumentType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetClinicVerificationService implements GetClinicVerificationUseCase {

    private static final int MAX_ATTEMPTS = 3;

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public GetClinicVerificationResponse getVerification(UUID veterinaryId) {
        authorizationService.requireVeterinaryOwner(veterinaryId);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        VerificationRequest request = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId)
                .orElseThrow(VerificationRequestNotFoundException::new);

        Map<String, VerificationDocument> documentsByType = verificationDocumentRepositoryPort.findByRequestId(request.getId())
                .stream()
                .collect(Collectors.toMap(VerificationDocument::getDocumentType, d -> d));

        List<VerificationDocumentStatusInfo> documents = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        for (ClinicDocumentType type : ClinicDocumentType.values()) {
            VerificationDocument doc = documentsByType.get(type.name());
            String status;
            if (doc == null) {
                status = "PENDING";
                if (type.isRequired()) {
                    missingRequired.add(type.name());
                }
            } else if (isUnderCorrection(request, type, doc)) {
                status = "NEEDS_CORRECTION";
            } else {
                status = "READY";
            }
            documents.add(VerificationDocumentStatusInfo.builder()
                    .documentType(type.name())
                    .required(type.isRequired())
                    .status(status)
                    .documentId(doc != null ? doc.getId() : null)
                    .fileName(doc != null ? doc.displayFileName() : null)
                    .uploadedAt(doc != null ? doc.getUploadedAt() : null)
                    .build());
        }

        boolean canEdit = request.getStatus() == VerificationRequestStatus.DRAFT
                || request.getStatus() == VerificationRequestStatus.NEEDS_CORRECTION;
        boolean legalRepComplete = isLegalRepComplete(veterinary);
        boolean canSubmit = canEdit && legalRepComplete && missingRequired.isEmpty();

        LegalRepresentativeInfo legalRepresentative = legalRepComplete || veterinary.getLegalRepName() != null
                ? LegalRepresentativeInfo.builder()
                        .isOwner(veterinary.getLegalRepIsOwner())
                        .fullName(veterinary.getLegalRepName())
                        .documentType(veterinary.getLegalRepDocumentType())
                        .documentNumber(veterinary.getLegalRepDocumentNumber())
                        .build()
                : null;

        AuthenticatedUser caller = authenticatedUserPort.current();
        Optional<ProfessionalProfile> profile = professionalProfileRepositoryPort.findByUserId(caller.userId());
        ProfessionalProfileSummary professionalProfile = ProfessionalProfileSummary.builder()
                .exists(profile.isPresent())
                .licenseStatus(profile.map(ProfessionalProfile::getLicenseStatus).orElse(null))
                .build();

        return GetClinicVerificationResponse.builder()
                .veterinaryId(veterinary.getId())
                .veterinaryStatus(veterinary.getStatus())
                .requestId(request.getId())
                .requestStatus(request.getStatus())
                .attemptNumber(request.getAttemptNumber())
                .maxAttempts(MAX_ATTEMPTS)
                .attemptsRemaining(MAX_ATTEMPTS - request.getAttemptNumber())
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt())
                .reviewerNotes(request.getReviewerNotes())
                .rejectionReason(null)
                .legalRepresentative(legalRepresentative)
                .documents(documents)
                .professionalProfile(professionalProfile)
                .canEdit(canEdit)
                .canSubmit(canSubmit)
                .missingRequired(missingRequired)
                .documentsDeletionDate(request.getStatus() == VerificationRequestStatus.REJECTED && request.getDecidedAt() != null
                        ? request.getDecidedAt().plusDays(90)
                        : null)
                .build();
    }

    private boolean isUnderCorrection(VerificationRequest request, ClinicDocumentType type, VerificationDocument doc) {
        if (request.getCorrectionDocumentTypes() == null || request.getCorrectionRequestedAt() == null) {
            return false;
        }
        return request.getCorrectionDocumentTypes().contains(type.name())
                && doc.getUploadedAt() != null
                && doc.getUploadedAt().isBefore(request.getCorrectionRequestedAt());
    }

    private boolean isLegalRepComplete(Veterinary veterinary) {
        return veterinary.getLegalRepIsOwner() != null
                && veterinary.getLegalRepName() != null && !veterinary.getLegalRepName().isBlank()
                && veterinary.getLegalRepDocumentType() != null && !veterinary.getLegalRepDocumentType().isBlank()
                && veterinary.getLegalRepDocumentNumber() != null && !veterinary.getLegalRepDocumentNumber().isBlank();
    }
}
