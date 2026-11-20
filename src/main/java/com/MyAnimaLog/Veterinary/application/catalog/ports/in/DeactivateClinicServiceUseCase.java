package com.MyAnimaLog.Veterinary.application.catalog.ports.in;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;

import java.util.UUID;

public interface DeactivateClinicServiceUseCase {
    ClinicServiceResponse deactivate(UUID veterinaryId, UUID serviceId);
}
