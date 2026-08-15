package com.MyAnimaLog.Veterinary.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IsExpiredResponse {
    private UUID veterinaryId;
    private String plan;
    private LocalDate endDate;
    private Boolean expired;
}
