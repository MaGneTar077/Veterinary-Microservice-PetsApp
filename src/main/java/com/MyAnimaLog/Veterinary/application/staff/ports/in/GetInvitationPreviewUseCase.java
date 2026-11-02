package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewResponse;

public interface GetInvitationPreviewUseCase {
    GetInvitationPreviewResponse preview(GetInvitationPreviewRequest request);
}
