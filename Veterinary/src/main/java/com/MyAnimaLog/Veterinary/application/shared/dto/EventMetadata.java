package com.MyAnimaLog.Veterinary.application.shared.dto;

import java.util.UUID;

public record EventMetadata(String version, String source, UUID veterinaryId, UUID correlationId) {

    private static final String VERSION = "1.0";
    private static final String SOURCE = "veterinary-service";

    public static EventMetadata of(UUID veterinaryId, UUID correlationId) {
        return new EventMetadata(VERSION, SOURCE, veterinaryId, correlationId);
    }
}
