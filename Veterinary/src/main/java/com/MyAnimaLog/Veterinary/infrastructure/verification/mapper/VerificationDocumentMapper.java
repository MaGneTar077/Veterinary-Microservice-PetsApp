package com.MyAnimaLog.Veterinary.infrastructure.verification.mapper;

import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.infrastructure.verification.entity.VerificationDocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class VerificationDocumentMapper {

    public VerificationDocumentEntity toEntity(VerificationDocument domain) {
        return VerificationDocumentEntity.builder()
                .id(domain.getId())
                .requestId(domain.getRequestId())
                .documentType(domain.getDocumentType())
                .storagePath(domain.getStoragePath())
                .mimeType(domain.getMimeType())
                .fileSizeBytes(domain.getFileSizeBytes())
                .sha256(domain.getSha256())
                .uploadedAt(domain.getUploadedAt())
                .build();
    }

    public VerificationDocument toDomain(VerificationDocumentEntity entity) {
        return VerificationDocument.builder()
                .id(entity.getId())
                .requestId(entity.getRequestId())
                .documentType(entity.getDocumentType())
                .storagePath(entity.getStoragePath())
                .mimeType(entity.getMimeType())
                .fileSizeBytes(entity.getFileSizeBytes())
                .sha256(entity.getSha256())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }
}
