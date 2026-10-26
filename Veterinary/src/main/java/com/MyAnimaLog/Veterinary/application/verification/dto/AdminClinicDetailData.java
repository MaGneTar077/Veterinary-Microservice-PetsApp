package com.MyAnimaLog.Veterinary.application.verification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminClinicDetailData {
    private String name;
    private String legalName;
    private String nit;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String department;
    private String logoUrl;
    private LegalRepresentativeInfo legalRepresentative;
}
