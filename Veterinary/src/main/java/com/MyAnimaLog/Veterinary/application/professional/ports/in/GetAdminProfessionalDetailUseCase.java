package com.MyAnimaLog.Veterinary.application.professional.ports.in;

import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalDetailResponse;

import java.util.UUID;

public interface GetAdminProfessionalDetailUseCase {
    AdminProfessionalDetailResponse getDetail(UUID profileId);
}
