package com.MyAnimaLog.Veterinary.application.shared.dto;

import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        VeterinaryEventType eventType,
        UUID userId,
        Recipient recipient,
        Map<String, Object> payload,
        Instant occurredAt,
        EventMetadata metadata
) {

    public static DomainEvent create(VeterinaryEventType eventType, UUID userId, Recipient recipient,
                                      Map<String, Object> payload, EventMetadata metadata) {
        if (userId == null && recipient == null) {
            throw new IllegalArgumentException(
                    "Invalid event: userId and recipient cannot both be null");
        }
        return new DomainEvent(eventType, userId, recipient, payload, Instant.now(), metadata);
    }
}
