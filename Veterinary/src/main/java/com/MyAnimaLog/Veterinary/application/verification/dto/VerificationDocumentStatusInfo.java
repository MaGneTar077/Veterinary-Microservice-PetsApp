package com.MyAnimaLog.Veterinary.application.verification.dto;

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
public class VerificationDocumentStatusInfo {
    private String documentType;
    private Boolean required;
    private String status;
    private UUID documentId;
    private String fileName;
    private LocalDateTime uploadedAt;
}
