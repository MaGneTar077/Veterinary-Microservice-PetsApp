package com.MyAnimaLog.Veterinary.application.clinic.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateLegalRepresentativeRequest {
    private Boolean isOwner;
    private String fullName;
    private String documentType;
    private String documentNumber;
}
