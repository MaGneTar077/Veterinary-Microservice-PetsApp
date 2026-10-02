package com.MyAnimaLog.Veterinary.application.subscription.ports.in;

import com.MyAnimaLog.Veterinary.application.subscription.dto.GetActivePlanResponse;

import java.util.UUID;

public interface GetActivePlanUseCase {
    GetActivePlanResponse getActivePlan(UUID veterinaryId);
}
