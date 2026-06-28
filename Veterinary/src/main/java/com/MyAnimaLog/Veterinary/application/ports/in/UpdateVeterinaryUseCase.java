package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryResponse;

import java.util.UUID;

public interface UpdateVeterinaryUseCase {
    UpdateVeterinaryResponse update(UUID veterinaryId, UpdateVeterinaryRequest request);
}
