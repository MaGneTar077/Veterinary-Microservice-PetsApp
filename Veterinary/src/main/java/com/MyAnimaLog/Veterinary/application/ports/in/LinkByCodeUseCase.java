package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeResponse;

public interface LinkByCodeUseCase {
    LinkByCodeResponse linkByCode(LinkByCodeRequest request);
}
