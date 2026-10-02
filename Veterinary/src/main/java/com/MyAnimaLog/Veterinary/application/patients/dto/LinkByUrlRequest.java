package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkByUrlRequest {
    private UUID userId;
    private String inviteLink;
}