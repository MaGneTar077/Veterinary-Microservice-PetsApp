package com.MyAnimaLog.Veterinary.application.verification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminVerificationDocumentInfo {
    private UUID documentId;
    private String documentType;
    private String fileName;
    private String mimeType;
    private Long fileSizeBytes;
    private LocalDateTime uploadedAt;
    private LocalDate issueDate;
    private String signedUrl;
    private LocalDateTime signedUrlExpiresAt;
}
