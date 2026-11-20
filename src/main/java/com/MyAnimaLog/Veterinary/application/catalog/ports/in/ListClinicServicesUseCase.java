package com.MyAnimaLog.Veterinary.application.catalog.ports.in;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ListClinicServicesResponse;

import java.util.UUID;

public interface ListClinicServicesUseCase {
    ListClinicServicesResponse list(UUID veterinaryId);
}
