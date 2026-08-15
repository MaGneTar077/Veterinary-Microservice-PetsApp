package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryResponse;

public interface RegisterVeterinaryUseCase {
    RegisterVeterinaryResponse registerVeterinary(RegisterVeterinaryRequest request);
}
