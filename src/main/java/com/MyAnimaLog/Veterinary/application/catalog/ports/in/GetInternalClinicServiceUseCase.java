package com.MyAnimaLog.Veterinary.application.catalog.ports.in;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;

import java.util.UUID;

public interface GetInternalClinicServiceUseCase {
    InternalClinicServiceResponse get(UUID veterinaryId, UUID serviceId);
}
