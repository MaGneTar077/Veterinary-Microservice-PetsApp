package com.MyAnimaLog.Veterinary.application.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListScheduleBlocksResponse {
    private List<ScheduleBlockResponse> items;
}
