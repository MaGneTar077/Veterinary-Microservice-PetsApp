package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;

import java.util.UUID;

public interface UnlinkMyVeterinaryUseCase {
    UnlinkResponse unlinkMine(UUID veterinaryId);
}
