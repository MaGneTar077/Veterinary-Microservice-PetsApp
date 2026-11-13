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

    record IaAnalysisRequest(UUID requestId, SubjectType subjectType, List<IaDocumentRef> documents,
                              Declared declared) {
    }

    record IaDocumentRef(String documentType, String signedUrl) {
    }

    /**
     * Datos declarados por la clínica al registrarse, solo para {@code subjectType=CLINIC} — permite
     * que la IA cruce lo declarado contra los documentos subidos. {@code null} en cada campo que la
     * clínica no tenga. Siempre {@code null} para {@code PROFESSIONAL}.
     */
    record Declared(String legalName, String nit, String legalRepresentativeName, String city) {
    }

    /** {@code reportJson} is the raw JSON body returned by the IA service, stored as-is in {@code ai_report}. */
    record IaAnalysisResult(String recommendation, String riskLevel, String reportJson) {
    }
}
