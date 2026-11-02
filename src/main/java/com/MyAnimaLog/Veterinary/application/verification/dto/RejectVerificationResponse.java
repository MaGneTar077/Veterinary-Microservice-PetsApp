package com.MyAnimaLog.Veterinary.application.verification.dto;

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
public class RejectVerificationResponse {
    private UUID requestId;
    private SubjectType subjectType;
    private VerificationRequestStatus status;
    private LocalDateTime decidedAt;
    private Integer attemptsRemaining;
    private LocalDateTime documentsDeletionDate;
}
