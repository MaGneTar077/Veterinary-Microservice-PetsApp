package com.MyAnimaLog.Veterinary.application.verification.dto;

import java.util.UUID;

/**
 * Internal Spring application event (not a {@code DomainEvent}/Pub-Sub envelope) used only to
 * trigger the async IA analysis after a verification request is submitted — picked up by
 * {@code VerificationAiAnalysisListener} via {@code @TransactionalEventListener(AFTER_COMMIT)}.
 */
public record VerificationSubmittedEvent(UUID requestId) {
}
