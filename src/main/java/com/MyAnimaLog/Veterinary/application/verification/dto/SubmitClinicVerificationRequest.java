package com.MyAnimaLog.Veterinary.application.verification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitClinicVerificationRequest {
    private Boolean truthfulnessDeclared;
    private Boolean includeProfessionalProfile;
}
