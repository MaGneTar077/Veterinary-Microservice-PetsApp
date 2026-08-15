package com.MyAnimaLog.Veterinary.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkByCodeRequest {
    private UUID userId;
    private String inviteCode;
}
