package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.RequestVerificationCorrectionRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.RequestVerificationCorrectionResponse;

import java.util.UUID;

public interface RequestVerificationCorrectionUseCase {
    RequestVerificationCorrectionResponse requestCorrection(UUID requestId, RequestVerificationCorrectionRequest request);
}
