package com.MyAnimaLog.Veterinary.application.verification.ports.out;

import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;

import java.util.List;
import java.util.UUID;

/**
 * Contract with the IA verification service (VETERINARY_SERVICE_PLAN.md §12, D10 in
 * FASE_2B_VERIFICACION.md). {@code isEnabled()} drives whether the async analysis listener even
 * attempts to call {@code analyze} — the {@code NoOpIaVerificationAdapter} always returns
 * {@code false} and never actually calls the IA service.
 */
public interface IaVerificationPort {

    boolean isEnabled();

    IaAnalysisResult analyze(IaAnalysisRequest request);

    record IaAnalysisRequest(UUID requestId, SubjectType subjectType, List<IaDocumentRef> documents) {
    }

    record IaDocumentRef(String documentType, String signedUrl) {
    }

    /** {@code reportJson} is the raw JSON body returned by the IA service, stored as-is in {@code ai_report}. */
    record IaAnalysisResult(String recommendation, String riskLevel, String reportJson) {
    }
}
