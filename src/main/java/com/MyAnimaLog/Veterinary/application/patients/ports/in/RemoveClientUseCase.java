package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;

import java.util.UUID;

public interface RemoveClientUseCase {
    UnlinkResponse remove(UUID veterinaryId, UUID userId);
}
