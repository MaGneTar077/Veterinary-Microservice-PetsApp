package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GenerateInviteCodeRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.GenerateInviteCodeResponse;

public interface GenerateInviteCodeUseCase {
    GenerateInviteCodeResponse generateInviteCode(GenerateInviteCodeRequest request);
}
