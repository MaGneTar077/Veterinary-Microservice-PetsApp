package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlResponse;

public interface LinkByUrlUseCase {
    LinkByUrlResponse linkByUrl(LinkByUrlRequest request);
}
