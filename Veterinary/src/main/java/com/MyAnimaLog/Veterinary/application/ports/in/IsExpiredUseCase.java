package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.IsExpiredResponse;

import java.util.UUID;

public interface IsExpiredUseCase {
    IsExpiredResponse isExpired(UUID veterinaryId);
}
