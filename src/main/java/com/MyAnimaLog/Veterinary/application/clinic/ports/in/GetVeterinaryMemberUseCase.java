package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryMemberResponse;

import java.util.UUID;

public interface GetVeterinaryMemberUseCase {
    GetVeterinaryMemberResponse getMember(UUID veterinaryId, UUID userId);
}
