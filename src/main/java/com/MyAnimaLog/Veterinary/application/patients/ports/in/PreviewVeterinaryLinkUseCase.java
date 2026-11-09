package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkResponse;

public interface PreviewVeterinaryLinkUseCase {
    PreviewVeterinaryLinkResponse preview(PreviewVeterinaryLinkRequest request);
}
