package com.MyAnimaLog.Veterinary.application.professional.ports.in;

import com.MyAnimaLog.Veterinary.application.professional.dto.UploadSignatureResponse;

public interface UploadProfessionalSignatureUseCase {
    UploadSignatureResponse uploadSignature(byte[] content);
}
