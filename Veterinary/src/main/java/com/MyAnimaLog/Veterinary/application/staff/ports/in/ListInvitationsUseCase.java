package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.ListInvitationsResponse;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;

import java.util.UUID;

public interface ListInvitationsUseCase {
    ListInvitationsResponse list(UUID veterinaryId, InvitationStatus status);
}
