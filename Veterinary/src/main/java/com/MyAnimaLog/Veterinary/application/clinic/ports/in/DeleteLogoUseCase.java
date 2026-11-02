package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteLogoResponse;

import java.util.UUID;

public interface DeleteLogoUseCase {
    DeleteLogoResponse deleteLogo(UUID veterinaryId);
}
