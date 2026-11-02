package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.GetClinicVerificationResponse;

import java.util.UUID;

public interface GetClinicVerificationUseCase {
    GetClinicVerificationResponse getVerification(UUID veterinaryId);
}
