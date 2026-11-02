package com.MyAnimaLog.Veterinary.application.professional.ports.in;

import com.MyAnimaLog.Veterinary.application.professional.dto.EmployeeProfessionalProfileResponse;

import java.util.UUID;

public interface GetEmployeeProfessionalProfileUseCase {
    EmployeeProfessionalProfileResponse getProfile(UUID veterinaryId, UUID employeeId);
}
