package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeResponse;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkToVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.LinkCodeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El alias {@code @Deprecated} de la ruta vieja {@code POST /api/veterinary/link/code}. Lo único que
 * hay que probar acá es que <strong>delega</strong>: toda la lógica (código → clínica
 * {@code ACTIVE}, idempotencia, snapshot, evento) está probada en
 * {@link LinkToVeterinaryServiceTest}, y tener acá una copia de esos casos solo haría creer que son
 * dos caminos independientes.
 */
@ExtendWith(MockitoExtension.class)
class LinkByCodeServiceTest {

    @Mock
    private LinkToVeterinaryUseCase linkToVeterinaryUseCase;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private LinkByCodeService linkByCodeService;

    private UUID userId;
    private UUID veterinaryId;
    private LinkToVeterinaryResponse delegateResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();

        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "ana@correo.com", true, false, Optional.empty()));

        delegateResponse = LinkToVeterinaryResponse.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .veterinaryName("Clínica El Bosque")
                .status(LinkStatus.LINKED)
                .linkedAt(LocalDateTime.now())
                .alreadyLinked(false)
                .build();
    }

    @Test
    void linkByCode_shouldDelegateToLinkToVeterinary_withTheSameCode() {
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class))).thenReturn(delegateResponse);

        linkByCodeService.linkByCode(LinkByCodeRequest.builder().inviteCode("ABCD2345").build());

        ArgumentCaptor<LinkToVeterinaryRequest> delegated =
                ArgumentCaptor.forClass(LinkToVeterinaryRequest.class);
        verify(linkToVeterinaryUseCase).link(delegated.capture());
        assertThat(delegated.getValue().getCode()).isEqualTo("ABCD2345");
    }

    @Test
    void linkByCode_shouldKeepTheOldResponseShape() {
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class))).thenReturn(delegateResponse);

        LinkByCodeResponse response = linkByCodeService.linkByCode(
                LinkByCodeRequest.builder().inviteCode("ABCD2345").build());

        assertThat(response.getId()).isEqualTo(delegateResponse.getId());
        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        // El userId no viene en el DTO nuevo (es siempre el del token), pero la respuesta vieja lo
        // incluía, así que el alias lo rellena.
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStatus()).isEqualTo("LINKED");
        assertThat(response.getLinkedAt()).isEqualTo(delegateResponse.getLinkedAt());
    }

    @Test
    void linkByCode_shouldReturn200_whenUserWasAlreadyLinked() {
        // Cambio de contrato heredado de la lógica nueva: antes era 409 USER_ALREADY_LINKED.
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class)))
                .thenReturn(LinkToVeterinaryResponse.builder()
                        .id(delegateResponse.getId())
                        .veterinaryId(veterinaryId)
                        .status(LinkStatus.LINKED)
                        .linkedAt(delegateResponse.getLinkedAt())
                        .alreadyLinked(true)
                        .build());

        LinkByCodeResponse response = linkByCodeService.linkByCode(
                LinkByCodeRequest.builder().inviteCode("ABCD2345").build());

        assertThat(response.getStatus()).isEqualTo("LINKED");
    }

    @Test
    void linkByCode_shouldPropagateLinkCodeNotFound_whenCodeDoesNotResolve() {
        // Antes esto era 400 INVALID_INVITE_CODE / 409 VETERINARY_NOT_ACTIVE; ahora los dos casos
        // son el mismo 404, para no poder enumerar clínicas probando códigos.
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class)))
                .thenThrow(new LinkCodeNotFoundException());

        assertThatThrownBy(() -> linkByCodeService.linkByCode(
                LinkByCodeRequest.builder().inviteCode("ABCD2345").build()))
                .isInstanceOf(LinkCodeNotFoundException.class);
    }

    @Test
    void linkByCode_shouldPropagateInvalidInviteCode_whenCodeIsMissing() {
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class)))
                .thenThrow(new InvalidInviteCodeException("Link code is required"));

        assertThatThrownBy(() -> linkByCodeService.linkByCode(
                LinkByCodeRequest.builder().inviteCode(null).build()))
                .isInstanceOf(InvalidInviteCodeException.class);
    }
}
