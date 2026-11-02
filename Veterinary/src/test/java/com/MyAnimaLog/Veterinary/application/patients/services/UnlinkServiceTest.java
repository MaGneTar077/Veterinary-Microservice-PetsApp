package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnlinkServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private UnlinkService unlinkService;

    private UUID userId;
    private UUID veterinaryId;
    private Veterinary veterinary;
    private UserVeterinaryLink existingLink;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();

        veterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        existingLink = UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(veterinaryId)
                .status("LINKED")
                .linkedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void unlink_shouldReturnResponse_whenLinkExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(linkRepositoryPort.findByUserIdAndVeterinaryId(userId, veterinaryId))
                .thenReturn(Optional.of(existingLink));
        doNothing().when(linkRepositoryPort).deleteById(any(UUID.class));

        UnlinkResponse response = unlinkService.unlink(userId, veterinaryId);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getMessage()).isEqualTo("User unlinked successfully");
    }

    @Test
    void unlink_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                unlinkService.unlink(userId, veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void unlink_shouldThrowUserNotLinkedException_whenLinkDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(linkRepositoryPort.findByUserIdAndVeterinaryId(userId, veterinaryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                unlinkService.unlink(userId, veterinaryId)
        ).isInstanceOf(UserNotLinkedException.class);
    }

    @Test
    void unlink_shouldCallDeleteById_once() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(linkRepositoryPort.findByUserIdAndVeterinaryId(userId, veterinaryId))
                .thenReturn(Optional.of(existingLink));
        doNothing().when(linkRepositoryPort).deleteById(any(UUID.class));

        unlinkService.unlink(userId, veterinaryId);

        verify(linkRepositoryPort, times(1)).deleteById(existingLink.getId());
    }

    @Test
    void unlink_shouldNeverCallDelete_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                unlinkService.unlink(userId, veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(linkRepositoryPort, never()).deleteById(any());
    }

    @Test
    void unlink_shouldNeverCallDelete_whenLinkNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(linkRepositoryPort.findByUserIdAndVeterinaryId(userId, veterinaryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                unlinkService.unlink(userId, veterinaryId)
        ).isInstanceOf(UserNotLinkedException.class);

        verify(linkRepositoryPort, never()).deleteById(any());
    }

    @Test
    void unlink_shouldReturn403_whenCallerIsNeitherSelfNorAuthorizedStaff() {
        doThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE))
                .when(authorizationService).requireSelfOrPermission(userId, veterinaryId, Permission.CLINIC_CONFIGURE);

        assertThatThrownBy(() ->
                unlinkService.unlink(userId, veterinaryId)
        ).isInstanceOf(InsufficientPermissionException.class);

        verify(linkRepositoryPort, never()).deleteById(any());
    }

    @Test
    void unlink_shouldCheckSelfOrPermission_beforeTouchingRepositories() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(linkRepositoryPort.findByUserIdAndVeterinaryId(userId, veterinaryId))
                .thenReturn(Optional.of(existingLink));

        unlinkService.unlink(userId, veterinaryId);

        verify(authorizationService, times(1))
                .requireSelfOrPermission(userId, veterinaryId, Permission.CLINIC_CONFIGURE);
    }
}