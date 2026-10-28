package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicDetailResponse;

import java.util.UUID;

public interface GetAdminClinicDetailUseCase {
    AdminClinicDetailResponse getDetail(UUID veterinaryId);
}
