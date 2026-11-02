package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UploadLogoResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.UploadLogoUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.FileTooLargeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.storage.FileSignatureDetector;
import com.MyAnimaLog.Veterinary.infrastructure.storage.FileSignatureDetector.DetectedType;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadLogoService implements UploadLogoUseCase {

    private static final long MAX_LOGO_SIZE_BYTES = 2L * 1024 * 1024;
    private static final Set<DetectedType> ALLOWED_TYPES = Set.of(DetectedType.PNG, DetectedType.JPEG, DetectedType.WEBP);

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;

    @Override
    public UploadLogoResponse uploadLogo(UUID veterinaryId, byte[] content, String contentType) {
        authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        if (content == null || content.length > MAX_LOGO_SIZE_BYTES) {
            throw new FileTooLargeException("Logo must be 2 MB or smaller");
        }
        DetectedType type = FileSignatureDetector.detect(content)
                .filter(ALLOWED_TYPES::contains)
                .orElseThrow(() -> new InvalidFileTypeException("Logo must be a PNG, JPEG, or WEBP image"));

        String path = logoPath(veterinaryId, type.extension());
        String newUrl = fileStoragePort.uploadPublic(storageProperties.getLogosBucket(), path, content, type.mimeType());

        String previousExtension = extractExtension(veterinary.getLogoUrl());
        if (previousExtension != null && !previousExtension.equals(type.extension())) {
            fileStoragePort.delete(storageProperties.getLogosBucket(), logoPath(veterinaryId, previousExtension));
        }

        Veterinary saved = veterinaryRepositoryPort.save(veterinary.toBuilder().logoUrl(newUrl).build());

        return UploadLogoResponse.builder()
                .veterinaryId(saved.getId())
                .logoUrl(saved.getLogoUrl())
                .build();
    }

    private String logoPath(UUID veterinaryId, String extension) {
        return "veterinaries/" + veterinaryId + "/logo." + extension;
    }

    private String extractExtension(String url) {
        if (url == null) {
            return null;
        }
        int dot = url.lastIndexOf('.');
        return dot >= 0 ? url.substring(dot + 1) : null;
    }
}
