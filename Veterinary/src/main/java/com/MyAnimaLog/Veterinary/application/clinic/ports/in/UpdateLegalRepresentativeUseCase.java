package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeResponse;

import java.util.UUID;

public interface UpdateLegalRepresentativeUseCase {
    UpdateLegalRepresentativeResponse updateLegalRepresentative(UUID veterinaryId, UpdateLegalRepresentativeRequest request);
}
