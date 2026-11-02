package com.MyAnimaLog.Veterinary.application.professional.dto;

import com.MyAnimaLog.Veterinary.application.verification.dto.VerificationDocumentStatusInfo;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
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
    private List<VerificationDocumentStatusInfo> documents;
    private RejectionReasonCode rejectionReasonCode;
}
