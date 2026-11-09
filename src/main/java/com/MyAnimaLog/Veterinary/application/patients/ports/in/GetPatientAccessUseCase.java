package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetPatientAccessResponse;

import java.util.UUID;

public interface GetPatientAccessUseCase {
    GetPatientAccessResponse getAccess(UUID veterinaryId, UUID petId, UUID userId);
}
