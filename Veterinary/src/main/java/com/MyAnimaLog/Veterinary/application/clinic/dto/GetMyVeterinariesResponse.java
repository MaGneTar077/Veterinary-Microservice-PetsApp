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
public class GetMyVeterinariesResponse {
    private List<MyVeterinarySummary> veterinaries;
}
