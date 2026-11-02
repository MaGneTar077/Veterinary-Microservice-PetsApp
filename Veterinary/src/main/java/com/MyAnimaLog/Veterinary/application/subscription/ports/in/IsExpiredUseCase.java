package com.MyAnimaLog.Veterinary.application.subscription.ports.in;

import com.MyAnimaLog.Veterinary.application.subscription.dto.IsExpiredResponse;

import java.util.UUID;

public interface IsExpiredUseCase {
    IsExpiredResponse isExpired(UUID veterinaryId);
}
