package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.RevokePetShareResponse;

import java.util.UUID;

public interface RevokePetShareUseCase {
    RevokePetShareResponse revoke(UUID veterinaryId, UUID petId);
}
