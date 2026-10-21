package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UploadLogoResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.FileTooLargeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadLogoServiceTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0};
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0};
    private static final byte[] NOT_AN_IMAGE = {1, 2, 3, 4, 5};

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private FileStoragePort fileStoragePort;

    private StorageProperties storageProperties;
    private UUID veterinaryId;
    private Veterinary veterinary;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        veterinaryId = UUID.randomUUID();
        veterinary = Veterinary.builder().id(veterinaryId).build();

        lenient().when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private UploadLogoService serviceWithProperties() {
        return new UploadLogoService(veterinaryRepositoryPort, authorizationService, fileStoragePort, storageProperties);
    }

    @Test
    void uploadLogo_shouldSucceed_forPng() {
        UploadLogoService svc = serviceWithProperties();
        when(fileStoragePort.uploadPublic(eq("veterinary-logos"), anyString(), any(), anyString()))
                .thenReturn("https://cdn/veterinaries/" + veterinaryId + "/logo.png");

        UploadLogoResponse response = svc.uploadLogo(veterinaryId, PNG_BYTES, "image/png");

        assertThat(response.getLogoUrl()).endsWith("logo.png");
        verify(fileStoragePort, never()).delete(anyString(), anyString());
    }

    @Test
    void uploadLogo_shouldSucceed_forJpeg() {
        UploadLogoService svc = serviceWithProperties();
        when(fileStoragePort.uploadPublic(anyString(), anyString(), any(), anyString()))
                .thenReturn("https://cdn/logo.jpg");

        assertThat(svc.uploadLogo(veterinaryId, JPEG_BYTES, "image/jpeg")).isNotNull();
    }

    @Test
    void uploadLogo_shouldDeleteOldFile_whenExtensionChanges() {
        UploadLogoService svc = serviceWithProperties();
        Veterinary withOldLogo = veterinary.toBuilder()
                .logoUrl("https://cdn/veterinaries/" + veterinaryId + "/logo.jpg")
                .build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(withOldLogo));
        when(fileStoragePort.uploadPublic(anyString(), anyString(), any(), anyString()))
                .thenReturn("https://cdn/veterinaries/" + veterinaryId + "/logo.png");

        svc.uploadLogo(veterinaryId, PNG_BYTES, "image/png");

        verify(fileStoragePort, times(1)).delete("veterinary-logos", "veterinaries/" + veterinaryId + "/logo.jpg");
    }

    @Test
    void uploadLogo_shouldNotDeleteOldFile_whenExtensionIsUnchanged() {
        UploadLogoService svc = serviceWithProperties();
        Veterinary withOldLogo = veterinary.toBuilder()
                .logoUrl("https://cdn/veterinaries/" + veterinaryId + "/logo.png")
                .build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(withOldLogo));
        when(fileStoragePort.uploadPublic(anyString(), anyString(), any(), anyString()))
                .thenReturn("https://cdn/veterinaries/" + veterinaryId + "/logo.png");

        svc.uploadLogo(veterinaryId, PNG_BYTES, "image/png");

        verify(fileStoragePort, never()).delete(anyString(), anyString());
    }

    @Test
    void uploadLogo_shouldThrowInvalidFileType_whenContentIsNotARealImage() {
        UploadLogoService svc = serviceWithProperties();

        assertThatThrownBy(() -> svc.uploadLogo(veterinaryId, NOT_AN_IMAGE, "image/png"))
                .isInstanceOf(InvalidFileTypeException.class);

        verify(fileStoragePort, never()).uploadPublic(any(), any(), any(), any());
    }

    @Test
    void uploadLogo_shouldThrowFileTooLarge_whenContentExceeds2Mb() {
        UploadLogoService svc = serviceWithProperties();
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG_BYTES, 0, big, 0, PNG_BYTES.length);

        assertThatThrownBy(() -> svc.uploadLogo(veterinaryId, big, "image/png"))
                .isInstanceOf(FileTooLargeException.class);
    }

    @Test
    void uploadLogo_shouldThrowVeterinaryNotFound_whenVeterinaryDoesNotExist() {
        UploadLogoService svc = serviceWithProperties();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.uploadLogo(veterinaryId, PNG_BYTES, "image/png"))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void uploadLogo_shouldPropagateNotVeterinaryOwner_whenCallerLacksAccess() {
        UploadLogoService svc = serviceWithProperties();
        doThrow(new NotVeterinaryOwnerException()).when(authorizationService)
                .requireOwnerOrPermission(eq(veterinaryId), any());

        assertThatThrownBy(() -> svc.uploadLogo(veterinaryId, PNG_BYTES, "image/png"))
                .isInstanceOf(NotVeterinaryOwnerException.class);

        verify(veterinaryRepositoryPort, never()).findById(any());
    }
}
