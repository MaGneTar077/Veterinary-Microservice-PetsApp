package com.MyAnimaLog.Veterinary.application.professional.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadSignatureResponse {
    private UUID profileId;
    private Boolean hasSignature;
}
