package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationResponse;

import java.util.UUID;

public interface SubmitClinicVerificationUseCase {
    SubmitClinicVerificationResponse submit(UUID veterinaryId, SubmitClinicVerificationRequest request);
}
