package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.verification.dto.VerificationSubmittedEvent;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.IaVerificationPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiRiskLevel;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Runs the IA analysis after the submitting transaction actually commits — so a rollback (e.g. the
 * joint clinic+profile submission failing) never leaves a stray "RUNNING" or triggers an analysis
 * for a submission that never really happened. Never changes {@code VerificationRequest.status};
 * only {@code ai_status}/{@code ai_report}/{@code ai_risk_level}. Never logs document content or
 * data the IA extracted — only identifiers and generic outcomes.
 */
@Component
@RequiredArgsConstructor
public class VerificationAiAnalysisListener {

    private static final Logger log = LoggerFactory.getLogger(VerificationAiAnalysisListener.class);
    private static final Set<String> VALID_RECOMMENDATIONS = Set.of("READY_FOR_HUMAN_REVIEW", "NEEDS_ATTENTION");
    private static final Duration SIGNED_URL_TTL = Duration.ofMinutes(10);

    private final IaVerificationPort iaVerificationPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationSubmitted(VerificationSubmittedEvent event) {
        if (!iaVerificationPort.isEnabled()) {
            return;
        }
        VerificationRequest request = verificationRequestRepositoryPort.findById(event.requestId()).orElse(null);
        if (request == null) {
            return;
        }

        verificationRequestRepositoryPort.save(request.toBuilder().aiStatus(AiStatus.RUNNING).build());

        try {
            List<IaVerificationPort.IaDocumentRef> documentRefs = verificationDocumentRepositoryPort
                    .findByRequestId(request.getId()).stream()
                    .map(this::toDocumentRef)
                    .toList();

            IaVerificationPort.IaAnalysisResult result = iaVerificationPort.analyze(
                    new IaVerificationPort.IaAnalysisRequest(request.getId(), request.getSubjectType(), documentRefs));

            if (result.recommendation() != null && !VALID_RECOMMENDATIONS.contains(result.recommendation())) {
                log.warn("IA returned an unexpected recommendation for requestId={}", request.getId());
            }

            verificationRequestRepositoryPort.save(request.toBuilder()
                    .aiStatus(AiStatus.DONE)
                    .aiReport(result.reportJson())
                    .aiRiskLevel(parseRiskLevel(result.riskLevel()))
                    .build());
        } catch (RuntimeException ex) {
            log.error("IA analysis failed for requestId={}", request.getId());
            verificationRequestRepositoryPort.save(request.toBuilder().aiStatus(AiStatus.FAILED).build());
        }
    }

    private IaVerificationPort.IaDocumentRef toDocumentRef(VerificationDocument document) {
        String signedUrl = fileStoragePort.createSignedUrl(
                storageProperties.getVerificationBucket(), document.getStoragePath(), SIGNED_URL_TTL);
        return new IaVerificationPort.IaDocumentRef(document.getDocumentType(), signedUrl);
    }

    private AiRiskLevel parseRiskLevel(String riskLevel) {
        if (riskLevel == null) {
            return null;
        }
        try {
            return AiRiskLevel.valueOf(riskLevel.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
