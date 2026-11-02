package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
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
public class GetClinicVerificationResponse {
    private UUID veterinaryId;
    private VeterinaryStatus veterinaryStatus;
    private UUID requestId;
    private VerificationRequestStatus requestStatus;
    private Integer attemptNumber;
    private Integer maxAttempts;
    private Integer attemptsRemaining;
    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
    private String reviewerNotes;
    private String rejectionReason;
    private RejectionReasonCode rejectionReasonCode;
    private LegalRepresentativeInfo legalRepresentative;
    private List<VerificationDocumentStatusInfo> documents;
    private ProfessionalProfileSummary professionalProfile;
    private Boolean canEdit;
    private Boolean canSubmit;
    private List<String> missingRequired;
    private LocalDateTime documentsDeletionDate;
}
