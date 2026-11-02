package com.MyAnimaLog.Veterinary.application.professional.ports.in;

import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalListResponse;

public interface ListAdminProfessionalsUseCase {
    AdminProfessionalListResponse list(String search, Boolean verified, Integer page, Integer size);
}
