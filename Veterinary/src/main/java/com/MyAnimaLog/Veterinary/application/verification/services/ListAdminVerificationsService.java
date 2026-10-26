package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationQueueItem;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationQueueResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.ListAdminVerificationsUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ListAdminVerificationsService implements ListAdminVerificationsUseCase {

    private static final int DEFAULT_SIZE = 20;

    private final VeterinaryAuthorizationService authorizationService;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Override
    public AdminVerificationQueueResponse list(VerificationRequestStatus status, SubjectType subjectType, Integer page, Integer size) {
        authorizationService.requirePlatformAdmin();

        VerificationRequestStatus effectiveStatus = status != null ? status : VerificationRequestStatus.UNDER_REVIEW;
        int effectivePage = page != null ? page : 0;
        int effectiveSize = size != null ? size : DEFAULT_SIZE;

        VerificationRequestRepositoryPort.AdminQueuePage queuePage =
                verificationRequestRepositoryPort.findForAdminQueue(effectiveStatus, subjectType, effectivePage, effectiveSize);

        List<AdminVerificationQueueItem> items = queuePage.items().stream()
                .map(this::toQueueItem)
                .toList();

        Map<VerificationRequestStatus, Long> countsByStatus = verificationRequestRepositoryPort.countByStatusForAdminQueue(subjectType);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (VerificationRequestStatus s : List.of(VerificationRequestStatus.UNDER_REVIEW, VerificationRequestStatus.NEEDS_CORRECTION,
                VerificationRequestStatus.APPROVED, VerificationRequestStatus.REJECTED)) {
            counts.put(s.name(), countsByStatus.getOrDefault(s, 0L));
        }

        return AdminVerificationQueueResponse.builder()
                .items(items)
                .page(effectivePage)
                .size(effectiveSize)
                .total(queuePage.total())
                .counts(counts)
                .build();
    }

    private AdminVerificationQueueItem toQueueItem(VerificationRequest request) {
        String displayName = null;
        String identifier = null;
        String profession = null;

        if (request.getSubjectType() == SubjectType.CLINIC) {
            Optional<Veterinary> veterinary = veterinaryRepositoryPort.findById(request.getSubjectId());
            displayName = veterinary.map(Veterinary::getName).orElse(null);
            identifier = veterinary.map(Veterinary::getNit).map(nit -> nit.value()).orElse(null);
        } else {
            Optional<ProfessionalProfile> profile = professionalProfileRepositoryPort.findById(request.getSubjectId());
            displayName = profile.map(ProfessionalProfile::getFullName).orElse(null);
            identifier = profile.map(ProfessionalProfile::getLicenseNumber).orElse(null);
            profession = profile.map(p -> p.getProfession() != null ? p.getProfession().name() : null).orElse(null);
        }

        return AdminVerificationQueueItem.builder()
                .requestId(request.getId())
                .subjectType(request.getSubjectType())
                .subjectId(request.getSubjectId())
                .displayName(displayName)
                .identifier(identifier)
                .profession(profession)
                .submittedAt(request.getSubmittedAt())
                .attemptNumber(request.getAttemptNumber())
                .status(request.getStatus())
                .aiStatus(request.getAiStatus())
                .aiRiskLevel(request.getAiRiskLevel())
                .duplicateFileAlert(hasDuplicateFile(request))
                .build();
    }

    private boolean hasDuplicateFile(VerificationRequest request) {
        List<VerificationDocument> documents = verificationDocumentRepositoryPort.findByRequestId(request.getId());
        for (VerificationDocument document : documents) {
            for (VerificationDocument other : verificationDocumentRepositoryPort.findBySha256(document.getSha256())) {
                if (other.getRequestId().equals(request.getId())) {
                    continue;
                }
                boolean belongsToOtherSubject = verificationRequestRepositoryPort.findById(other.getRequestId())
                        .map(otherRequest -> !otherRequest.getSubjectId().equals(request.getSubjectId()))
                        .orElse(false);
                if (belongsToOtherSubject) {
                    return true;
                }
            }
        }
        return false;
    }
}
