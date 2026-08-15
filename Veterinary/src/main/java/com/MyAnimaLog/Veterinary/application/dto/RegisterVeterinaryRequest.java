package com.MyAnimaLog.Veterinary.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterVeterinaryRequest {
    private String name;
    private String city;
    private String phone;
    private String email;
}
