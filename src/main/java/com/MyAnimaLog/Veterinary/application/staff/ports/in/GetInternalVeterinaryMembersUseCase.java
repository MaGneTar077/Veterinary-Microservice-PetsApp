package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.InternalVeterinaryMemberResponse;

import java.util.List;
import java.util.UUID;

public interface GetInternalVeterinaryMembersUseCase {
    List<InternalVeterinaryMemberResponse> list(UUID veterinaryId, boolean includeInactive);
}
