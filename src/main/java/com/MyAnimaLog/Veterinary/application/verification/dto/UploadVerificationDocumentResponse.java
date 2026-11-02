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
public class UploadVerificationDocumentResponse {
    private UUID documentId;
    private UUID requestId;
    private String documentType;
    private String fileName;
    private LocalDateTime uploadedAt;
}
