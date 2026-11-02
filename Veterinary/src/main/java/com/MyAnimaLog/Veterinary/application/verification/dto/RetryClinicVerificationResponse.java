package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RetryClinicVerificationResponse {
    private UUID veterinaryId;
    private UUID requestId;
    private VerificationRequestStatus status;
    private Integer attemptNumber;
}
