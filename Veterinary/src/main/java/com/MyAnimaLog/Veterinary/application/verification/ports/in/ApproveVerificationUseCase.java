package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.ApproveVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.ApproveVerificationResponse;

import java.util.UUID;

public interface ApproveVerificationUseCase {
    ApproveVerificationResponse approve(UUID requestId, ApproveVerificationRequest request);
}
