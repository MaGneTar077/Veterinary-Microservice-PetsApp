package com.MyAnimaLog.Veterinary.application.catalog.ports.in;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.dto.CreateClinicServiceRequest;

import java.util.UUID;

public interface CreateClinicServiceUseCase {
    ClinicServiceResponse create(UUID veterinaryId, CreateClinicServiceRequest request);
}
