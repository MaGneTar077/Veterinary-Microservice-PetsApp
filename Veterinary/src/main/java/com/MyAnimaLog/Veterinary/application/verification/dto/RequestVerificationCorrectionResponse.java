package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestVerificationCorrectionResponse {
    private UUID requestId;
    private SubjectType subjectType;
    private VerificationRequestStatus status;
    private List<String> correctionDocumentTypes;
    private LocalDateTime correctionRequestedAt;
}
