package com.MyAnimaLog.Veterinary.application.subscription.ports.in;

import com.MyAnimaLog.Veterinary.application.subscription.dto.UpdatePlanRequest;
import com.MyAnimaLog.Veterinary.application.subscription.dto.UpdatePlanResponse;

import java.util.UUID;

public interface UpdatePlanUseCase {
    UpdatePlanResponse updatePlan(UUID veterinaryId, UpdatePlanRequest request);
}
