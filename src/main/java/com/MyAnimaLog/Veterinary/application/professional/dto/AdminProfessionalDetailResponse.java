package com.MyAnimaLog.Veterinary.application.professional.dto;

import com.MyAnimaLog.Veterinary.application.verification.dto.AdminWorkplaceInfo;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminProfessionalDetailResponse {
    private UUID id;
    private String fullName;
    private String email;
    private IdDocumentType documentType;
    private String documentNumber;
    private String licenseNumber;
    private Profession profession;
    private String specialty;
    private String university;
    private LicenseStatus licenseStatus;
    private String rejectionReason;
    private LocalDateTime verifiedAt;
    private List<AdminWorkplaceInfo> workplaces;
    private LocalDateTime createdAt;
}
