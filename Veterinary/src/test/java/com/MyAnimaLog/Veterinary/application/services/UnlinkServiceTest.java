package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
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
}