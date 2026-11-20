package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetInternalVeterinaryDetailsResponse;

import java.util.UUID;

public interface GetInternalVeterinaryDetailsUseCase {
    GetInternalVeterinaryDetailsResponse get(UUID veterinaryId);
}
