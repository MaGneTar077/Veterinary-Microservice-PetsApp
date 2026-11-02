package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationDetailResponse;

import java.util.UUID;

public interface GetAdminVerificationDetailUseCase {
    AdminVerificationDetailResponse getDetail(UUID requestId);
}
