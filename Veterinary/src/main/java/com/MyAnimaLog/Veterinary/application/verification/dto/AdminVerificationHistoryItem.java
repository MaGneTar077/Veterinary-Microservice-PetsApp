package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminVerificationHistoryItem {
    private Integer attemptNumber;
    private VerificationRequestStatus status;
    private RejectionReasonCode rejectionReasonCode;
    private String reviewerNotes;
    private LocalDateTime decidedAt;
}
