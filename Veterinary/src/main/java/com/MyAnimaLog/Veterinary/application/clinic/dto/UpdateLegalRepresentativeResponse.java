package com.MyAnimaLog.Veterinary.application.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLegalRepresentativeResponse {
    private UUID veterinaryId;
    private Boolean isOwner;
    private String fullName;
    private String documentType;
    private String documentNumber;
}
