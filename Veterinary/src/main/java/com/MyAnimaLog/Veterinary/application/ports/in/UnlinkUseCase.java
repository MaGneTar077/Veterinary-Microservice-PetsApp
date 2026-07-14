package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.UnlinkResponse;

import java.util.UUID;

public interface UnlinkUseCase {
    UnlinkResponse unlink(UUID userId, UUID veterinaryId);
}
