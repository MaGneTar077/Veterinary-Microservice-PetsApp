package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetSharedWithVeterinaryResponse;

import java.util.UUID;

public interface GetSharedWithVeterinaryUseCase {
    GetSharedWithVeterinaryResponse get(UUID veterinaryId, UUID petId);
}
