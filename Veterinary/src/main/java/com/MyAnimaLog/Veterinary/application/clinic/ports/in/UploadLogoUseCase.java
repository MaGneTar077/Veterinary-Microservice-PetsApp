package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UploadLogoResponse;

import java.util.UUID;

public interface UploadLogoUseCase {
    UploadLogoResponse uploadLogo(UUID veterinaryId, byte[] content, String contentType);
}
