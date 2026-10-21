package com.MyAnimaLog.Veterinary.domain.legal.model;

import com.MyAnimaLog.Veterinary.domain.legal.enums.ConsentType;
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
public class LegalConsent {
    private UUID id;
    private UUID userId;
    private UUID veterinaryId;
    private ConsentType consentType;
    private String version;
    @Builder.Default
    private LocalDateTime acceptedAt = LocalDateTime.now();
}
