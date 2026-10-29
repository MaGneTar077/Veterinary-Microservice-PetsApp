package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.LeaveVeterinaryResponse;

import java.util.UUID;

public interface LeaveVeterinaryUseCase {
    LeaveVeterinaryResponse leave(UUID veterinaryId);
}
