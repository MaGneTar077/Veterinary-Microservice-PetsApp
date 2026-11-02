package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.RevokeInvitationResponse;

import java.util.UUID;

public interface RevokeInvitationUseCase {
    RevokeInvitationResponse revoke(UUID veterinaryId, UUID invitationId);
}
