package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteInviteCodeResponse;

import java.util.UUID;

public interface DeleteInviteCodeUseCase {
    DeleteInviteCodeResponse deleteInviteCode(UUID veterinaryId);
}
