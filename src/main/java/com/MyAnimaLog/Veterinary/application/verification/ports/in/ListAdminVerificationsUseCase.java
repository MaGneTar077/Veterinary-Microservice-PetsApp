package com.MyAnimaLog.Veterinary.application.verification.ports.in;

import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationQueueResponse;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;

public interface ListAdminVerificationsUseCase {
    AdminVerificationQueueResponse list(VerificationRequestStatus status, SubjectType subjectType, Integer page, Integer size);
}
