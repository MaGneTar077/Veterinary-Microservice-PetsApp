package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.RerunVerificationAnalysisResponse;

import java.util.UUID;

public interface RerunVerificationAnalysisUseCase {
    RerunVerificationAnalysisResponse rerun(UUID requestId);
}
