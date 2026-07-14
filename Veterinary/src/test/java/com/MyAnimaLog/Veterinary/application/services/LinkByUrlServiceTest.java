package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.LinkByUrlRequest;
import com.MyAnimaLog.Veterinary.application.dto.LinkByUrlResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.exceptions.UserAlreadyLinkedException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotActiveException;
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
class LinkByUrlServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @InjectMocks
    private LinkByUrlService linkByUrlService;

    private UUID userId;
    private Veterinary activeVeterinary;
    private Veterinary inactiveVeterinary;
    private UserVeterinaryLink savedLink;
    private LinkByUrlRequest validRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        UUID veterinaryId = UUID.randomUUID();

        activeVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .inviteLink("http://localhost:8082/api/veterinary/join?code=VET-DNU4WXDQ")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        inactiveVeterinary = activeVeterinary.toBuilder()
                .active(false)
                .build();

        savedLink = UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(veterinaryId)
                .status("LINKED")
                .linkedAt(LocalDateTime.now())
                .build();

        validRequest = LinkByUrlRequest.builder()
                .userId(userId)
                .inviteLink("http://localhost:8082/api/veterinary/join?code=VET-DNU4WXDQ")
                .build();
    }

    @Test
    void linkByUrl_shouldReturnResponse_whenRequestIsValid() {
        when(veterinaryRepositoryPort.findByInviteLink(any())).thenReturn(Optional.of(activeVeterinary));
        when(linkRepositoryPort.existsByUserIdAndVeterinaryId(any(), any())).thenReturn(false);
        when(linkRepositoryPort.save(any(UserVeterinaryLink.class))).thenReturn(savedLink);

        LinkByUrlResponse response = linkByUrlService.linkByUrl(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStatus()).isEqualTo("LINKED");
        assertThat(response.getLinkedAt()).isNotNull();
        assertThat(response.getVeterinaryId()).isEqualTo(activeVeterinary.getId());
    }

    @Test
    void linkByUrl_shouldThrowInvalidInviteCodeException_whenLinkIsNull() {
        validRequest.setInviteLink(null);

        assertThatThrownBy(() ->
                linkByUrlService.linkByUrl(validRequest)
        ).isInstanceOf(InvalidInviteCodeException.class);
    }

    @Test
    void linkByUrl_shouldThrowInvalidInviteCodeException_whenLinkIsBlank() {
        validRequest.setInviteLink("   ");

        assertThatThrownBy(() ->
                linkByUrlService.linkByUrl(validRequest)
        ).isInstanceOf(InvalidInviteCodeException.class);
    }

    @Test
    void linkByUrl_shouldThrowInvalidInviteCodeException_whenLinkDoesNotExist() {
        when(veterinaryRepositoryPort.findByInviteLink(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                linkByUrlService.linkByUrl(validRequest)
        ).isInstanceOf(InvalidInviteCodeException.class);
    }

    @Test
    void linkByUrl_shouldThrowVeterinaryNotActiveException_whenVeterinaryIsInactive() {
        when(veterinaryRepositoryPort.findByInviteLink(any())).thenReturn(Optional.of(inactiveVeterinary));

        assertThatThrownBy(() ->
                linkByUrlService.linkByUrl(validRequest)
        ).isInstanceOf(VeterinaryNotActiveException.class);
    }

    @Test
    void linkByUrl_shouldThrowUserAlreadyLinkedException_whenUserAlreadyLinked() {
        when(veterinaryRepositoryPort.findByInviteLink(any())).thenReturn(Optional.of(activeVeterinary));
        when(linkRepositoryPort.existsByUserIdAndVeterinaryId(any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
                linkByUrlService.linkByUrl(validRequest)
        ).isInstanceOf(UserAlreadyLinkedException.class);
    }

    @Test
    void linkByUrl_shouldCallSave_once() {
        when(veterinaryRepositoryPort.findByInviteLink(any())).thenReturn(Optional.of(activeVeterinary));
        when(linkRepositoryPort.existsByUserIdAndVeterinaryId(any(), any())).thenReturn(false);
        when(linkRepositoryPort.save(any(UserVeterinaryLink.class))).thenReturn(savedLink);

        linkByUrlService.linkByUrl(validRequest);

        verify(linkRepositoryPort, times(1)).save(any(UserVeterinaryLink.class));
    }

    @Test
    void linkByUrl_shouldNeverCallSave_whenLinkIsInvalid() {
        when(veterinaryRepositoryPort.findByInviteLink(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                linkByUrlService.linkByUrl(validRequest)
        ).isInstanceOf(InvalidInviteCodeException.class);

        verify(linkRepositoryPort, never()).save(any());
    }

    @Test
    void linkByUrl_shouldNeverCallSave_whenUserAlreadyLinked() {
        when(veterinaryRepositoryPort.findByInviteLink(any())).thenReturn(Optional.of(activeVeterinary));
        when(linkRepositoryPort.existsByUserIdAndVeterinaryId(any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
                linkByUrlService.linkByUrl(validRequest)
        ).isInstanceOf(UserAlreadyLinkedException.class);

        verify(linkRepositoryPort, never()).save(any());
    }
}