package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationResponse;

public interface DeclineInvitationUseCase {
    DeclineInvitationResponse decline(DeclineInvitationRequest request);
}
