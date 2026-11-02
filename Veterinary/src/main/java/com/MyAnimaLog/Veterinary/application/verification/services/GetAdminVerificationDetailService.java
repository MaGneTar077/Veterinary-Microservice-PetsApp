package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminAccountInfo;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminAiInfo;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminClinicDetailData;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminDuplicateInfo;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminProfessionalDetailData;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationDetailResponse;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationDocumentInfo;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationHistoryItem;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminWorkplaceInfo;
import com.MyAnimaLog.Veterinary.application.verification.dto.LegalRepresentativeInfo;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.GetAdminVerificationDetailUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAdminVerificationDetailService implements GetAdminVerificationDetailUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetAdminVerificationDetailService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration SIGNED_URL_TTL = Duration.ofMinutes(10);

    private final VeterinaryAuthorizationService authorizationService;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final UserServicePort userServicePort;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;

    @Value("${app.verification.rues-url}")
    private String ruesUrl;

    @Value("${app.verification.comvezcol-url}")
    private String comvezcolUrl;

    @Override
    public AdminVerificationDetailResponse getDetail(UUID requestId) {
        authorizationService.requirePlatformAdmin();

        VerificationRequest request = verificationRequestRepositoryPort.findById(requestId)
                .orElseThrow(VerificationRequestNotFoundException::new);

        AdminClinicDetailData clinicData = null;
        AdminProfessionalDetailData professionalData = null;
        if (request.getSubjectType() == SubjectType.CLINIC) {
            clinicData = buildClinicData(request.getSubjectId());
        } else {
            professionalData = buildProfessionalData(request.getSubjectId());
        }

        AdminAccountInfo account = resolveAccount(request.getSubmittedBy());

        List<VerificationDocument> documents = verificationDocumentRepositoryPort.findByRequestId(requestId);
        Map<String, Object> report = parseAiReport(request.getAiReport());

        List<AdminVerificationDocumentInfo> documentInfos = documents.stream()
                .map(doc -> toDocumentInfo(doc, report))
                .toList();

        AdminAiInfo ai = AdminAiInfo.builder()
                .aiStatus(request.getAiStatus())
                .aiRiskLevel(request.getAiRiskLevel())
                .report(report)
                .build();

        List<AdminDuplicateInfo> duplicates = findDuplicates(request, documents);

        List<AdminVerificationHistoryItem> history = verificationRequestRepositoryPort
                .findBySubjectTypeAndSubjectId(request.getSubjectType(), request.getSubjectId()).stream()
                .filter(r -> !r.getId().equals(requestId))
                .map(r -> AdminVerificationHistoryItem.builder()
                        .attemptNumber(r.getAttemptNumber())
                        .status(r.getStatus())
                        .rejectionReasonCode(r.getRejectionReasonCode())
                        .reviewerNotes(r.getReviewerNotes())
                        .decidedAt(r.getDecidedAt())
                        .build())
                .toList();

        return AdminVerificationDetailResponse.builder()
                .requestId(request.getId())
                .subjectType(request.getSubjectType())
                .subjectId(request.getSubjectId())
                .status(request.getStatus())
                .clinic(clinicData)
                .professional(professionalData)
                .account(account)
                .attemptNumber(request.getAttemptNumber())
                .maxAttempts(MAX_ATTEMPTS)
                .attemptsRemaining(MAX_ATTEMPTS - request.getAttemptNumber())
                .documents(documentInfos)
                .ai(ai)
                .duplicates(duplicates)
                .history(history)
                .registryLookupUrl(request.getSubjectType() == SubjectType.CLINIC ? ruesUrl : comvezcolUrl)
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt())
                .build();
    }

    private AdminClinicDetailData buildClinicData(UUID veterinaryId) {
        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);
        return AdminClinicDetailData.builder()
                .name(veterinary.getName())
                .legalName(veterinary.getLegalName())
                .nit(veterinary.getNit() != null ? veterinary.getNit().value() : null)
                .phone(veterinary.getPhone())
                .email(veterinary.getEmail())
                .address(veterinary.getAddress())
                .city(veterinary.getCity())
                .department(veterinary.getDepartment())
                .logoUrl(veterinary.getLogoUrl())
                .legalRepresentative(LegalRepresentativeInfo.builder()
                        .isOwner(veterinary.getLegalRepIsOwner())
                        .fullName(veterinary.getLegalRepName())
                        .documentType(veterinary.getLegalRepDocumentType())
                        .documentNumber(veterinary.getLegalRepDocumentNumber())
                        .build())
                .build();
    }

    private AdminProfessionalDetailData buildProfessionalData(UUID profileId) {
        ProfessionalProfile profile = professionalProfileRepositoryPort.findById(profileId)
                .orElseThrow(ProfessionalProfileNotFoundException::new);
        List<AdminWorkplaceInfo> workplaces = employeeRepositoryPort.findByUserIdAndActiveTrue(profile.getUserId()).stream()
                .map(employee -> AdminWorkplaceInfo.builder()
                        .veterinaryId(employee.getVeterinaryId())
                        .name(veterinaryRepositoryPort.findById(employee.getVeterinaryId()).map(Veterinary::getName).orElse(null))
                        .role(employee.getRole())
                        .build())
                .toList();
        return AdminProfessionalDetailData.builder()
                .fullName(profile.getFullName())
                .documentType(profile.getDocumentType())
                .documentNumber(profile.getDocumentNumber())
                .profession(profile.getProfession())
                .licenseNumber(profile.getLicenseNumber())
                .specialty(profile.getSpecialty())
                .university(profile.getUniversity())
                .workplaces(workplaces)
                .build();
    }

    private AdminAccountInfo resolveAccount(UUID userId) {
        try {
            UserServicePort.UserInfo info = userServicePort.getUser(userId);
            return AdminAccountInfo.builder()
                    .id(info.id())
                    .email(info.email())
                    .emailVerified(info.emailVerified())
                    .build();
        } catch (RuntimeException ex) {
            log.warn("User service lookup failed for userId={}, falling back to id-only account info", userId);
            return AdminAccountInfo.builder().id(userId).build();
        }
    }

    private AdminVerificationDocumentInfo toDocumentInfo(VerificationDocument document, Map<String, Object> report) {
        String bucket = storageProperties.getVerificationBucket();
        String signedUrl = fileStoragePort.createSignedUrl(bucket, document.getStoragePath(), SIGNED_URL_TTL);
        return AdminVerificationDocumentInfo.builder()
                .documentId(document.getId())
                .documentType(document.getDocumentType())
                .fileName(document.displayFileName())
                .mimeType(document.getMimeType())
                .fileSizeBytes(document.getFileSizeBytes())
                .uploadedAt(document.getUploadedAt())
                .issueDate(extractIssueDate(report, document.getDocumentType()))
                .signedUrl(signedUrl)
                .signedUrlExpiresAt(LocalDateTime.now().plus(SIGNED_URL_TTL))
                .build();
    }

    @SuppressWarnings("unchecked")
    private LocalDate extractIssueDate(Map<String, Object> report, String documentType) {
        if (report == null) {
            return null;
        }
        Object documentsRaw = report.get("documents");
        if (!(documentsRaw instanceof List<?> documentsList)) {
            return null;
        }
        for (Object entry : documentsList) {
            if (!(entry instanceof Map<?, ?> entryMap)) {
                continue;
            }
            if (!documentType.equals(entryMap.get("documentType"))) {
                continue;
            }
            Object issueDate = entryMap.get("issueDate");
            if (issueDate == null) {
                return null;
            }
            try {
                return LocalDate.parse(issueDate.toString());
            } catch (RuntimeException ex) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAiReport(String aiReport) {
        if (aiReport == null || aiReport.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(aiReport, Map.class);
        } catch (JsonProcessingException ex) {
            log.warn("Could not parse ai_report as JSON for a verification request");
            return null;
        }
    }

    private List<AdminDuplicateInfo> findDuplicates(VerificationRequest request, List<VerificationDocument> documents) {
        List<AdminDuplicateInfo> duplicates = new java.util.ArrayList<>();
        for (VerificationDocument document : documents) {
            for (VerificationDocument other : verificationDocumentRepositoryPort.findBySha256(document.getSha256())) {
                if (other.getRequestId().equals(request.getId())) {
                    continue;
                }
                verificationRequestRepositoryPort.findById(other.getRequestId())
                        .filter(otherRequest -> !otherRequest.getSubjectId().equals(request.getSubjectId()))
                        .ifPresent(otherRequest -> duplicates.add(AdminDuplicateInfo.builder()
                                .documentType(document.getDocumentType())
                                .otherSubjectType(otherRequest.getSubjectType())
                                .otherSubjectName(resolveSubjectName(otherRequest))
                                .build()));
            }
        }
        return duplicates;
    }

    private String resolveSubjectName(VerificationRequest otherRequest) {
        if (otherRequest.getSubjectType() == SubjectType.CLINIC) {
            return veterinaryRepositoryPort.findById(otherRequest.getSubjectId()).map(Veterinary::getName).orElse(null);
        }
        return professionalProfileRepositoryPort.findById(otherRequest.getSubjectId())
                .map(ProfessionalProfile::getFullName).orElse(null);
    }
}
