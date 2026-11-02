package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RejectVerificationRequest {
    private RejectionReasonCode reasonCode;
    private String notes;
}
