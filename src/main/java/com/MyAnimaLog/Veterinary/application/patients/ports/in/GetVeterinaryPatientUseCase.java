package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinaryPatientResponse;

import java.util.UUID;

public interface GetVeterinaryPatientUseCase {
    GetVeterinaryPatientResponse get(UUID veterinaryId, UUID petId);
}
