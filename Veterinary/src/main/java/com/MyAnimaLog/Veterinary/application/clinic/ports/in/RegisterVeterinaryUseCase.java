package com.MyAnimaLog.Veterinary.application.clinic.ports.in;

import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryResponse;

public interface RegisterVeterinaryUseCase {
    RegisterVeterinaryResponse registerVeterinary(RegisterVeterinaryRequest request);
}
