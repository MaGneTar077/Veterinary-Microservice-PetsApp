package com.MyAnimaLog.Veterinary.application.verification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminVerificationQueueResponse {
    private List<AdminVerificationQueueItem> items;
    private int page;
    private int size;
    private long total;
    private Map<String, Long> counts;
}
