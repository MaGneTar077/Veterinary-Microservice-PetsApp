package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.LinkByUrlRequest;
import com.MyAnimaLog.Veterinary.application.dto.LinkByUrlResponse;

public interface LinkByUrlUseCase {
    LinkByUrlResponse linkByUrl(LinkByUrlRequest request);
}
