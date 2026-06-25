package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.GenerateInviteCodeRequest;
import com.MyAnimaLog.Veterinary.application.dto.GenerateInviteCodeResponse;

public interface GenerateInviteCodeUseCase {
    GenerateInviteCodeResponse generateInviteCode(GenerateInviteCodeRequest request);
}
