package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicListResponse;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;

public interface ListAdminClinicsUseCase {
    AdminClinicListResponse list(VeterinaryStatus status, String search, String plan, String city,
                                  String department, Integer page, Integer size);
}
