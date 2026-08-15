package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.DeActivateVeterinaryResponse;

import java.util.UUID;

public interface DeActivateVeterinaryUseCase {
    DeActivateVeterinaryResponse deActivate(UUID veterinaryId);
}
