package com.MyAnimaLog.Veterinary.application.verification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LegalRepresentativeInfo {
    private Boolean isOwner;
    private String fullName;
    private String documentType;
    private String documentNumber;
}
