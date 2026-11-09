package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetMyLinkedVeterinariesResponse;

public interface GetMyLinkedVeterinariesUseCase {
    GetMyLinkedVeterinariesResponse list();
}
