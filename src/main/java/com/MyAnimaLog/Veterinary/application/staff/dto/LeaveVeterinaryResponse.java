package com.MyAnimaLog.Veterinary.application.staff.dto;

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
public class LeaveVeterinaryResponse {
    private UUID employeeId;
    private UUID veterinaryId;
    private LocalDateTime removedAt;
}
