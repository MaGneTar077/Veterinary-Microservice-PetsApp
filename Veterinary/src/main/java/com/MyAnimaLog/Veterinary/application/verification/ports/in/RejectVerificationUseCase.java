package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.RejectVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.RejectVerificationResponse;

import java.util.UUID;

public interface RejectVerificationUseCase {
    RejectVerificationResponse reject(UUID requestId, RejectVerificationRequest request);
}
