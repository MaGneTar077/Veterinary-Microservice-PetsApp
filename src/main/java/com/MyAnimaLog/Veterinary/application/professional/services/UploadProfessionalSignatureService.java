package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.UploadSignatureResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.UploadProfessionalSignatureUseCase;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.FileTooLargeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
import com.MyAnimaLog.Veterinary.infrastructure.storage.FileSignatureDetector;
import com.MyAnimaLog.Veterinary.infrastructure.storage.FileSignatureDetector.DetectedType;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UploadProfessionalSignatureService implements UploadProfessionalSignatureUseCase {

    private static final long MAX_SIGNATURE_SIZE_BYTES = 1L * 1024 * 1024;

    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;

    @Override
    public UploadSignatureResponse uploadSignature(byte[] content) {
        AuthenticatedUser caller = authenticatedUserPort.current();
        ProfessionalProfile profile = professionalProfileRepositoryPort.findByUserId(caller.userId())
                .orElseThrow(ProfessionalProfileNotFoundException::new);

        if (content == null || content.length > MAX_SIGNATURE_SIZE_BYTES) {
            throw new FileTooLargeException("Signature must be 1 MB or smaller");
        }
        DetectedType type = FileSignatureDetector.detect(content)
                .filter(t -> t == DetectedType.PNG)
                .orElseThrow(() -> new InvalidFileTypeException("Signature must be a PNG image"));

        String path = "professionals/" + profile.getId() + "/signature.png";
        fileStoragePort.uploadPrivate(storageProperties.getVerificationBucket(), path, content, type.mimeType());

        professionalProfileRepositoryPort.save(profile.toBuilder()
                .signatureUrl(path)
                .updatedAt(LocalDateTime.now())
                .build());

        return UploadSignatureResponse.builder()
                .profileId(profile.getId())
                .hasSignature(true)
                .build();
    }
}
