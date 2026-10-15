package com.MyAnimaLog.Veterinary.domain.professional.model;

import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ProfessionalProfile {
    private UUID id;
    private UUID userId;
    private String fullName;
    private IdDocumentType documentType;
    private String documentNumber;
    private String licenseNumber;
    private Profession profession;
    private String specialty;
    private String university;
    private String signatureUrl;
    private LicenseStatus licenseStatus;
    private String rejectionReason;
    private LocalDateTime verifiedAt;
    private UUID verifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
