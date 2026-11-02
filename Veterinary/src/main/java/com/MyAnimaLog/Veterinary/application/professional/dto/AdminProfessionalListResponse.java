package com.MyAnimaLog.Veterinary.application.professional.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminProfessionalListResponse {
    private List<AdminProfessionalListItem> items;
    private int page;
    private int size;
    private long total;
}
