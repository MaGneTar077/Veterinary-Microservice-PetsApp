package com.MyAnimaLog.Veterinary.application.verification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestVerificationCorrectionRequest {
    private String notes;
    private List<String> documentTypes;
}
