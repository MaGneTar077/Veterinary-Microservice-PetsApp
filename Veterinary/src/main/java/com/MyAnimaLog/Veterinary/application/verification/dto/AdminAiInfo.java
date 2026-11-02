package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.verification.enums.AiRiskLevel;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminAiInfo {
    private AiStatus aiStatus;
    private AiRiskLevel aiRiskLevel;
    private Map<String, Object> report;
}
