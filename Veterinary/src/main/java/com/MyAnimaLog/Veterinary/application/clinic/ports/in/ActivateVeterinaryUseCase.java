package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.ActivateVeterinaryResponse;

import java.util.UUID;

public interface ActivateVeterinaryUseCase {
    ActivateVeterinaryResponse activate(UUID veterinaryId);
}
