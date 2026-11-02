package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsResponse;

import java.util.UUID;

public interface UpdateVeterinarySettingsUseCase {
    UpdateVeterinarySettingsResponse updateSettings(UUID veterinaryId, UpdateVeterinarySettingsRequest request);
}
