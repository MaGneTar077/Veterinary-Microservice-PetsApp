package com.MyAnimaLog.Veterinary.application.catalog.ports.in;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;

import java.util.List;
import java.util.UUID;

public interface GetInternalClinicServicesUseCase {
    List<InternalClinicServiceResponse> list(UUID veterinaryId, boolean activeOnly);
}
