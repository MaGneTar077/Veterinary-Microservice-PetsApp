package com.MyAnimaLog.Veterinary.infrastructure.ai;

import com.MyAnimaLog.Veterinary.application.verification.ports.out.IaVerificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Registered when {@code AI_VERIFICATION_ENABLED=false} (the default) — {@code isEnabled()}
 * returns {@code false} so {@code VerificationAiAnalysisListener} never calls {@code analyze},
 * which is why {@code analyze} itself just throws if it's ever reached.
 */
@Component
@ConditionalOnProperty(name = "app.ai.verification-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpIaVerificationAdapter implements IaVerificationPort {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public IaAnalysisResult analyze(IaAnalysisRequest request) {
        throw new UnsupportedOperationException("AI verification is disabled");
    }
}
