package com.MyAnimaLog.Veterinary.application.professional.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpsertProfessionalProfileRequest {
    private String fullName;
    private String documentType;
    private String documentNumber;
    private String profession;
    private String licenseNumber;
    private String specialty;
    private String university;
}
