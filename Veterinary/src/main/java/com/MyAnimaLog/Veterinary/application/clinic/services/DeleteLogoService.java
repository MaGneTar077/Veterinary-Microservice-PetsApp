package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteLogoResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.DeleteLogoUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteLogoService implements DeleteLogoUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;

    @Override
    public DeleteLogoResponse deleteLogo(UUID veterinaryId) {
        authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        if (veterinary.getLogoUrl() != null) {
            String extension = extractExtension(veterinary.getLogoUrl());
            fileStoragePort.delete(storageProperties.getLogosBucket(), "veterinaries/" + veterinaryId + "/logo." + extension);
        }

        Veterinary saved = veterinaryRepositoryPort.save(veterinary.toBuilder().logoUrl(null).build());

        return DeleteLogoResponse.builder()
                .veterinaryId(saved.getId())
                .logoUrl(saved.getLogoUrl())
                .build();
    }

    private String extractExtension(String url) {
        int dot = url.lastIndexOf('.');
        return dot >= 0 ? url.substring(dot + 1) : null;
    }
}
