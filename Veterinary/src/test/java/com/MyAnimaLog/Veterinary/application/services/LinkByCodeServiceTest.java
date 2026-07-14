package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeResponse;
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
class LinkByCodeServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @InjectMocks
    private LinkByCodeService linkByCodeService;

    private UUID userId;
    private Veterinary activeVeterinary;
    private Veterinary inactiveVeterinary;
    private UserVeterinaryLink savedLink;
    private LinkByCodeRequest validRequest;

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
                .inviteCode("VET-DNU4WXDQ")
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

        validRequest = LinkByCodeRequest.builder()
                .userId(userId)
                .inviteCode("VET-DNU4WXDQ")
                .build();
    }

    @Test
    void linkByCode_shouldReturnResponse_whenRequestIsValid() {
        when(veterinaryRepositoryPort.findByInviteCode("VET-DNU4WXDQ"))
                .thenReturn(Optional.of(activeVeterinary));
        when(linkRepositoryPort.existsByUserIdAndVeterinaryId(any(), any())).thenReturn(false);
        when(linkRepositoryPort.save(any(UserVeterinaryLink.class))).thenReturn(savedLink);

        LinkByCodeResponse response = linkByCodeService.linkByCode(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStatus()).isEqualTo("LINKED");
        assertThat(response.getLinkedAt()).isNotNull();
        assertThat(response.getVeterinaryId()).isEqualTo(activeVeterinary.getId());
    }

    @Test
    void linkByCode_shouldThrowInvalidInviteCodeException_whenCodeIsNull() {
        validRequest.setInviteCode(null);

        assertThatThrownBy(() ->
                linkByCodeService.linkByCode(validRequest)
        ).isInstanceOf(InvalidInviteCodeException.class);
    }

    @Test
    void linkByCode_shouldThrowInvalidInviteCodeException_whenCodeIsBlank() {
        validRequest.setInviteCode("   ");

        assertThatThrownBy(() ->
                linkByCodeService.linkByCode(validRequest)
        ).isInstanceOf(InvalidInviteCodeException.class);
    }

    @Test
    void linkByCode_shouldThrowInvalidInviteCodeException_whenCodeDoesNotExist() {
        when(veterinaryRepositoryPort.findByInviteCode(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                linkByCodeService.linkByCode(validRequest)
        ).isInstanceOf(InvalidInviteCodeException.class);
    }

    @Test
    void linkByCode_shouldThrowVeterinaryNotActiveException_whenVeterinaryIsInactive() {
        when(veterinaryRepositoryPort.findByInviteCode("VET-DNU4WXDQ"))
                .thenReturn(Optional.of(inactiveVeterinary));

        assertThatThrownBy(() ->
                linkByCodeService.linkByCode(validRequest)
        ).isInstanceOf(VeterinaryNotActiveException.class);
    }

    @Test
    void linkByCode_shouldThrowUserAlreadyLinkedException_whenUserAlreadyLinked() {
        when(veterinaryRepositoryPort.findByInviteCode("VET-DNU4WXDQ"))
                .thenReturn(Optional.of(activeVeterinary));
        when(linkRepositoryPort.existsByUserIdAndVeterinaryId(any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
                linkByCodeService.linkByCode(validRequest)
        ).isInstanceOf(UserAlreadyLinkedException.class);
    }

    @Test
    void linkByCode_shouldCallSave_once() {
        when(veterinaryRepositoryPort.findByInviteCode("VET-DNU4WXDQ"))
                .thenReturn(Optional.of(activeVeterinary));
        when(linkRepositoryPort.existsByUserIdAndVeterinaryId(any(), any())).thenReturn(false);
        when(linkRepositoryPort.save(any(UserVeterinaryLink.class))).thenReturn(savedLink);

        linkByCodeService.linkByCode(validRequest);

        verify(linkRepositoryPort, times(1)).save(any(UserVeterinaryLink.class));
    }

    @Test
    void linkByCode_shouldNeverCallSave_whenCodeIsInvalid() {
        when(veterinaryRepositoryPort.findByInviteCode(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                linkByCodeService.linkByCode(validRequest)
        ).isInstanceOf(InvalidInviteCodeException.class);

        verify(linkRepositoryPort, never()).save(any());
    }

    @Test
    void linkByCode_shouldNeverCallSave_whenUserAlreadyLinked() {
        when(veterinaryRepositoryPort.findByInviteCode("VET-DNU4WXDQ"))
                .thenReturn(Optional.of(activeVeterinary));
        when(linkRepositoryPort.existsByUserIdAndVeterinaryId(any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
                linkByCodeService.linkByCode(validRequest)
        ).isInstanceOf(UserAlreadyLinkedException.class);

        verify(linkRepositoryPort, never()).save(any());
    }
}