package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.ResendInvitationResponse;

import java.util.UUID;

public interface ResendInvitationUseCase {
    ResendInvitationResponse resend(UUID veterinaryId, UUID invitationId);
}
