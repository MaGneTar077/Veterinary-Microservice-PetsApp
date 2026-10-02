package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeActivateVeterinaryResponse;

import java.util.UUID;

public interface DeActivateVeterinaryUseCase {
    DeActivateVeterinaryResponse deActivate(UUID veterinaryId);
}
