package com.MyAnimaLog.Veterinary.domain.verification.model;

import com.MyAnimaLog.Veterinary.domain.verification.enums.AiRiskLevel;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
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
    private Integer attemptNumber;
    private UUID submittedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime truthfulnessDeclaredAt;
    private AiStatus aiStatus;
    private String aiReport;
    private AiRiskLevel aiRiskLevel;
    private UUID reviewerId;
    private String reviewerNotes;
    private LocalDateTime decidedAt;
    private Boolean registryChecked;
    private List<String> correctionDocumentTypes;
    private LocalDateTime correctionRequestedAt;
    private LocalDateTime createdAt;
}
