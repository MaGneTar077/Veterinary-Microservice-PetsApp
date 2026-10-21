package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.RetryClinicVerificationResponse;

import java.util.UUID;

public interface RetryClinicVerificationUseCase {
    RetryClinicVerificationResponse retry(UUID veterinaryId);
}
