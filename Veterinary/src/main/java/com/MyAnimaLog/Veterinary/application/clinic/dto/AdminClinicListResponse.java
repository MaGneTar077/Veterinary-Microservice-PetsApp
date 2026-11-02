package com.MyAnimaLog.Veterinary.application.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminClinicListResponse {
    private List<AdminClinicListItem> items;
    private int page;
    private int size;
    private long total;
}
