package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationResponse;

public interface AcceptInvitationUseCase {
    AcceptInvitationResponse accept(AcceptInvitationRequest request);
}
