package com.MyAnimaLog.Veterinary.application.verification.dto;

import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminProfessionalDetailData {
    private String fullName;
    private IdDocumentType documentType;
    private String documentNumber;
    private Profession profession;
    private String licenseNumber;
    private String specialty;
    private String university;
    private List<AdminWorkplaceInfo> workplaces;
}
