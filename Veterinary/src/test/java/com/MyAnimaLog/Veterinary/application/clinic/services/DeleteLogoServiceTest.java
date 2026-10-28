package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteLogoResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicSuspendedException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteLogoServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private FileStoragePort fileStoragePort;

    private DeleteLogoService service;
    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        service = new DeleteLogoService(veterinaryRepositoryPort, authorizationService, fileStoragePort, new StorageProperties());
        veterinaryId = UUID.randomUUID();
        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void deleteLogo_shouldDeleteFileAndClearUrl_whenLogoExists() {
        Veterinary veterinary = Veterinary.builder().id(veterinaryId)
                .logoUrl("https://cdn/veterinaries/" + veterinaryId + "/logo.png").build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));

        DeleteLogoResponse response = service.deleteLogo(veterinaryId);

        assertThat(response.getLogoUrl()).isNull();
        verify(fileStoragePort, times(1)).delete("veterinary-logos", "veterinaries/" + veterinaryId + "/logo.png");
    }

    @Test
    void deleteLogo_shouldNotCallDelete_whenNoLogoExists() {
        Veterinary veterinary = Veterinary.builder().id(veterinaryId).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));

        service.deleteLogo(veterinaryId);

        verify(fileStoragePort, never()).delete(any(), any());
    }

    @Test
    void deleteLogo_shouldThrowVeterinaryNotFound_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteLogo(veterinaryId))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void deleteLogo_shouldPropagateClinicSuspended_whenClinicIsSuspended() {
        doThrow(new ClinicSuspendedException()).when(authorizationService)
                .requireOwnerOrPermission(eq(veterinaryId), any());

        assertThatThrownBy(() -> service.deleteLogo(veterinaryId))
                .isInstanceOf(ClinicSuspendedException.class);

        verify(veterinaryRepositoryPort, never()).findById(any());
        verify(fileStoragePort, never()).delete(any(), any());
    }
}
