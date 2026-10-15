package com.MyAnimaLog.Veterinary.application.legal.ports.out;

import com.MyAnimaLog.Veterinary.domain.legal.model.LegalConsent;

import java.util.List;
import java.util.UUID;

public interface LegalConsentRepositoryPort {
    List<LegalConsent> findByUserId(UUID userId);
    LegalConsent save(LegalConsent consent);
}
