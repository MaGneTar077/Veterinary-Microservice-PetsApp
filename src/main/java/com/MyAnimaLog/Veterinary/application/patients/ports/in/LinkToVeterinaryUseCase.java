package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryResponse;

public interface LinkToVeterinaryUseCase {
    LinkToVeterinaryResponse link(LinkToVeterinaryRequest request);
}
