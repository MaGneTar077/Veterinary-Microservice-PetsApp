package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.CreatePlanRequest;
import com.MyAnimaLog.Veterinary.application.dto.CreatePlanResponse;

public interface CreatePlanUseCase {
    CreatePlanResponse createPlan(CreatePlanRequest request);
}
