package com.MyAnimaLog.Veterinary.application.professional.ports.in;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.dto.UpsertProfessionalProfileRequest;

public interface UpsertProfessionalProfileUseCase {
    ProfessionalProfileResponse upsert(UpsertProfessionalProfileRequest request);
}
