package com.MyAnimaLog.Veterinary.application.professional.dto;

import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfessionalProfileResponse {
    private UUID id;
    private String fullName;
    private IdDocumentType documentType;
    private String documentNumber;
    private Profession profession;
    private String licenseNumber;
    private String specialty;
    private String university;
    private LicenseStatus licenseStatus;
}
