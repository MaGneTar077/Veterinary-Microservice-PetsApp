package com.MyAnimaLog.Veterinary.application.professional.dto;

import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeProfessionalProfileResponse {
    private boolean exists;
    private String fullName;
    private Profession profession;
    private String licenseNumber;
    private String specialty;
    private String university;
    private LicenseStatus licenseStatus;
}
