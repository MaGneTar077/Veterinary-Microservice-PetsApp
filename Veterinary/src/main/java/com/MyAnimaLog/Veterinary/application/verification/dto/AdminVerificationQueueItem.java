package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.verification.enums.AiRiskLevel;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminVerificationQueueItem {
    private UUID requestId;
    private SubjectType subjectType;
    private UUID subjectId;
    private String displayName;
    private String identifier;
    private String profession;
    private LocalDateTime submittedAt;
    private Integer attemptNumber;
    private VerificationRequestStatus status;
    private AiStatus aiStatus;
    private AiRiskLevel aiRiskLevel;
    private Boolean duplicateFileAlert;
}
