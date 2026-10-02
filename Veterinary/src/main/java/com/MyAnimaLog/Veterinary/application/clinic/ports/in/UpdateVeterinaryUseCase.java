package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinaryResponse;

import java.util.UUID;

public interface UpdateVeterinaryUseCase {
    UpdateVeterinaryResponse update(UUID veterinaryId, UpdateVeterinaryRequest request);
}
