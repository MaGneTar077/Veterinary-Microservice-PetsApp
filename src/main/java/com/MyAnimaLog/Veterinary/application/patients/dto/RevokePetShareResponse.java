package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code alreadyRevoked = true} significa que el consentimiento ya estaba revocado y no se tocó
 * nada (ni se publicó evento). Revocar dos veces no es un error: el resultado que el dueño pidió
 * —que la clínica no vea la mascota— ya se cumple.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RevokePetShareResponse {
    private UUID veterinaryId;
    private UUID petId;
    private String petName;
    private LocalDateTime revokedAt;
    private boolean alreadyRevoked;
}
