package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeResponse;

public interface LinkByCodeUseCase {
    LinkByCodeResponse linkByCode(LinkByCodeRequest request);
}
