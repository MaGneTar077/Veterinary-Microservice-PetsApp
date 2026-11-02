package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminVerificationDetailResponse {
    private UUID requestId;
    private SubjectType subjectType;
    private UUID subjectId;
    private VerificationRequestStatus status;

    private AdminClinicDetailData clinic;
    private AdminProfessionalDetailData professional;

    private AdminAccountInfo account;

    private Integer attemptNumber;
    private Integer maxAttempts;
    private Integer attemptsRemaining;

    private List<AdminVerificationDocumentInfo> documents;
    private AdminAiInfo ai;
    private List<AdminDuplicateInfo> duplicates;
    private List<AdminVerificationHistoryItem> history;

    private String registryLookupUrl;

    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
}
