package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipResponse;

import java.util.UUID;

public interface TransferOwnershipUseCase {
    TransferOwnershipResponse transfer(UUID veterinaryId, TransferOwnershipRequest request);
}
