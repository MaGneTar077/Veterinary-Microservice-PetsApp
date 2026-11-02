package com.MyAnimaLog.Veterinary.application.subscription.ports.in;

import com.MyAnimaLog.Veterinary.application.subscription.dto.CreatePlanRequest;
import com.MyAnimaLog.Veterinary.application.subscription.dto.CreatePlanResponse;

public interface CreatePlanUseCase {
    CreatePlanResponse createPlan(CreatePlanRequest request);
}
