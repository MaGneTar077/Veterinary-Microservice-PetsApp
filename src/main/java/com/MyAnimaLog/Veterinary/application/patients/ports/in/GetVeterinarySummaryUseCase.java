package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinarySummaryResponse;

import java.util.UUID;

public interface GetVeterinarySummaryUseCase {
    GetVeterinarySummaryResponse getSummary(UUID veterinaryId);
}
