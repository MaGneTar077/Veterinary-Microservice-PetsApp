package com.MyAnimaLog.Veterinary.infrastructure.verification.entity;

import com.MyAnimaLog.Veterinary.domain.verification.enums.AiRiskLevel;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import com.MyAnimaLog.Veterinary.domain.verification.enums.RejectionReasonCode;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "verification_requests")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationRequestEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false)
    private SubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VerificationRequestStatus status;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "truthfulness_declared_at")
    private LocalDateTime truthfulnessDeclaredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", nullable = false)
    private AiStatus aiStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_report")
    private String aiReport;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_risk_level")
    private AiRiskLevel aiRiskLevel;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "reviewer_notes")
    private String reviewerNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason_code")
    private RejectionReasonCode rejectionReasonCode;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "registry_checked", nullable = false)
    private Boolean registryChecked;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "correction_document_types")
    private List<String> correctionDocumentTypes;

    @Column(name = "correction_requested_at")
    private LocalDateTime correctionRequestedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
