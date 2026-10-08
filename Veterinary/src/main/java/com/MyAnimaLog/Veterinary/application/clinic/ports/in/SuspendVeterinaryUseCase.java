package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryResponse;

import java.util.UUID;

public interface SuspendVeterinaryUseCase {
    SuspendVeterinaryResponse suspend(UUID veterinaryId);
}
