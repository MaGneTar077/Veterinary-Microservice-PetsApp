package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryResponse;

import java.util.UUID;

public interface SharePetsWithVeterinaryUseCase {
    SharePetsWithVeterinaryResponse share(UUID veterinaryId, SharePetsWithVeterinaryRequest request);
}
