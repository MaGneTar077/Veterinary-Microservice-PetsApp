package com.MyAnimaLog.Veterinary.domain.verification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class VerificationDocument {
    private UUID id;
    private UUID requestId;
    private String documentType;
    private String storagePath;
    private String mimeType;
    private Long fileSizeBytes;
    private String sha256;
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
    private String originalFileName;

    /** What the clinic/professional sees as the file's name — never the raw storage path. */
    public String displayFileName() {
        if (originalFileName != null && !originalFileName.isBlank()) {
            return originalFileName;
        }
        if (storagePath == null) {
            return null;
        }
        int slash = storagePath.lastIndexOf('/');
        return slash >= 0 ? storagePath.substring(slash + 1) : storagePath;
    }
}
