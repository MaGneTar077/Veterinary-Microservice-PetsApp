package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.UpdatePlanRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdatePlanResponse;

import java.util.UUID;

public interface UpdatePlanUseCase {
    UpdatePlanResponse updatePlan(UUID veterinaryId, UpdatePlanRequest request);
}
