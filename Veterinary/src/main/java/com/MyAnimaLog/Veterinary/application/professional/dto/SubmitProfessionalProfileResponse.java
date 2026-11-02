package com.MyAnimaLog.Veterinary.application.professional.dto;

import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubmitProfessionalProfileResponse {
    private UUID profileId;
    private UUID requestId;
    private VerificationRequestStatus status;
    private LicenseStatus licenseStatus;
    private LocalDateTime submittedAt;
}
