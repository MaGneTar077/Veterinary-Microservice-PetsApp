package com.MyAnimaLog.Veterinary.domain.model;

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
public class UserVeterinaryLink {
    private UUID id;
    private UUID userId;
    private UUID veterinaryId;
    private String status;
    private LocalDateTime linkedAt;
}
