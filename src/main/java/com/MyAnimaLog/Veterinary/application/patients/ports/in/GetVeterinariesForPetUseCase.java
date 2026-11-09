package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinariesForPetResponse;

import java.util.UUID;

public interface GetVeterinariesForPetUseCase {
    GetVeterinariesForPetResponse list(UUID petId);
}
