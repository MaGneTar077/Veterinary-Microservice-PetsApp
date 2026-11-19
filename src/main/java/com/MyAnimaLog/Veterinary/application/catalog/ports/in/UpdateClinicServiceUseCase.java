package com.MyAnimaLog.Veterinary.application.catalog.ports.in;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.dto.UpdateClinicServiceRequest;

import java.util.UUID;

public interface UpdateClinicServiceUseCase {
    ClinicServiceResponse update(UUID veterinaryId, UUID serviceId, UpdateClinicServiceRequest request);
}
