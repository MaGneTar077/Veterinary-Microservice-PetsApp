package com.MyAnimaLog.Veterinary.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkByUrlResponse {
    private UUID id;
    private UUID userId;
    private UUID veterinaryId;
    private String status;
    private LocalDateTime linkedAt;
}