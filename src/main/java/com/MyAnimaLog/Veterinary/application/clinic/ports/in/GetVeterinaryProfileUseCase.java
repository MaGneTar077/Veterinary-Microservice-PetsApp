package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryProfileResponse;

import java.util.UUID;

public interface GetVeterinaryProfileUseCase {
    GetVeterinaryProfileResponse getProfile(UUID veterinaryId);
}
