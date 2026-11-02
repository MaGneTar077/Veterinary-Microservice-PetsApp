package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeResponse;

import java.util.UUID;

public interface InviteEmployeeUseCase {
    InviteEmployeeResponse invite(UUID veterinaryId, InviteEmployeeRequest request);
}
