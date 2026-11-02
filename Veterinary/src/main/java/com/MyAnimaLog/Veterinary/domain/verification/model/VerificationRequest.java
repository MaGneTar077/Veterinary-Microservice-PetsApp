package com.MyAnimaLog.Veterinary.domain.verification.model;

import com.MyAnimaLog.Veterinary.domain.verification.enums.AiRiskLevel;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
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
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class VerificationRequest {
    private UUID id;
    private SubjectType subjectType;
    private UUID subjectId;
    private VerificationRequestStatus status;
    @Builder.Default
    private Integer attemptNumber = 1;
    private UUID submittedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime truthfulnessDeclaredAt;
    @Builder.Default
    private AiStatus aiStatus = AiStatus.NOT_RUN;
    private String aiReport;
    private AiRiskLevel aiRiskLevel;
    private UUID reviewerId;
    private String reviewerNotes;
    private RejectionReasonCode rejectionReasonCode;
    private LocalDateTime decidedAt;
    @Builder.Default
    private Boolean registryChecked = false;
    private List<String> correctionDocumentTypes;
    private LocalDateTime correctionRequestedAt;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
