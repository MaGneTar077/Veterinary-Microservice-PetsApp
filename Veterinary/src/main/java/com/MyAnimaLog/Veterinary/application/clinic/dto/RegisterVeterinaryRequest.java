package com.MyAnimaLog.Veterinary.application.clinic.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RegisterVeterinaryRequest {
    private String name;
    private String city;
    private String phone;
    private String email;
    private String legalName;
    private String nit;
    private String address;
    private String department;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
