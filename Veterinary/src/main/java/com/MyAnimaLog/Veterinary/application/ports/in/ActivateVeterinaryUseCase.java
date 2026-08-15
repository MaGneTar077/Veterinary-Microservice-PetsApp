package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.ActivateVeterinaryResponse;

import java.util.UUID;

public interface ActivateVeterinaryUseCase {
    ActivateVeterinaryResponse activate(UUID veterinaryId);
}
