package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RerunVerificationAnalysisResponse {
    private UUID requestId;
    private AiStatus aiStatus;
}
