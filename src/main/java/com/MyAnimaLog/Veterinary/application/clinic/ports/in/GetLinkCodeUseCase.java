package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetLinkCodeResponse;

import java.util.UUID;

public interface GetLinkCodeUseCase {
    GetLinkCodeResponse getLinkCode(UUID veterinaryId);
}
