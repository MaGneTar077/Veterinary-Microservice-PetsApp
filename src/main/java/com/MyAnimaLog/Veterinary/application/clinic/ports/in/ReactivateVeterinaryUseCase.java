package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.ReactivateVeterinaryResponse;

import java.util.UUID;

public interface ReactivateVeterinaryUseCase {
    ReactivateVeterinaryResponse reactivate(UUID veterinaryId);
}
