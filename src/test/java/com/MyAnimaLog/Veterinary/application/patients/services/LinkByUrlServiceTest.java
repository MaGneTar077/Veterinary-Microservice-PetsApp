package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlResponse;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkToVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.LinkCodeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.*;

/**
 * Alias {@code @Deprecated} de {@code POST /api/veterinary/link/url}. Resuelve el enlace a su
 * clínica y delega con el código de esa clínica; el resto de la lógica se prueba en
 * {@link LinkToVeterinaryServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class LinkByUrlServiceTest {

    private static final String LINK = "https://app.myanimalog.com/clinicas/vincular?code=ABCD2345";

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private LinkToVeterinaryUseCase linkToVeterinaryUseCase;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private LinkByUrlService linkByUrlService;

    private UUID userId;
    private UUID veterinaryId;
    private Veterinary clinic;
    private LinkToVeterinaryResponse delegateResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();

        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "ana@correo.com", true, false, Optional.empty()));

        clinic = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .inviteCode("ABCD2345")
                .inviteLink(LINK)
                .build();

        delegateResponse = LinkToVeterinaryResponse.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .veterinaryName("Clínica El Bosque")
                .status(LinkStatus.LINKED)
                .linkedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void linkByUrl_shouldResolveTheLinkAndDelegateWithThatClinicCode() {
        when(veterinaryRepositoryPort.findByInviteLink(LINK)).thenReturn(Optional.of(clinic));
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class))).thenReturn(delegateResponse);

        LinkByUrlResponse response = linkByUrlService.linkByUrl(
                LinkByUrlRequest.builder().inviteLink(LINK).build());

        // Resuelve por enlace (así el enlace tiene que ser uno realmente emitido) y delega con el
        // código, en vez de parsear el "code=" de la URL.
        ArgumentCaptor<LinkToVeterinaryRequest> delegated =
                ArgumentCaptor.forClass(LinkToVeterinaryRequest.class);
        verify(linkToVeterinaryUseCase).link(delegated.capture());
        assertThat(delegated.getValue().getCode()).isEqualTo("ABCD2345");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getStatus()).isEqualTo("LINKED");
    }

    @Test
    void linkByUrl_shouldThrowLinkCodeNotFound_whenLinkIsUnknown() {
        when(veterinaryRepositoryPort.findByInviteLink(LINK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkByUrlService.linkByUrl(
                LinkByUrlRequest.builder().inviteLink(LINK).build()))
                .isInstanceOf(LinkCodeNotFoundException.class);

        verify(linkToVeterinaryUseCase, never()).link(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void linkByUrl_shouldThrowInvalidInviteCode_whenLinkIsBlank(String link) {
        assertThatThrownBy(() -> linkByUrlService.linkByUrl(
                LinkByUrlRequest.builder().inviteLink(link).build()))
                .isInstanceOf(InvalidInviteCodeException.class);

        verify(linkToVeterinaryUseCase, never()).link(any());
    }

    @Test
    void linkByUrl_shouldThrowInvalidInviteCode_whenLinkIsNull() {
        assertThatThrownBy(() -> linkByUrlService.linkByUrl(
                LinkByUrlRequest.builder().inviteLink(null).build()))
                .isInstanceOf(InvalidInviteCodeException.class);

        verify(linkToVeterinaryUseCase, never()).link(any());
    }

    @Test
    void linkByUrl_shouldPropagateLinkCodeNotFound_whenTheClinicIsNotActive() {
        // La resolución por enlace no mira el estado: lo rechaza el servicio nuevo, un solo lugar.
        when(veterinaryRepositoryPort.findByInviteLink(LINK)).thenReturn(Optional.of(
                clinic.toBuilder().status(VeterinaryStatus.SUSPENDED).active(false).build()));
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class)))
                .thenThrow(new LinkCodeNotFoundException());

        assertThatThrownBy(() -> linkByUrlService.linkByUrl(
                LinkByUrlRequest.builder().inviteLink(LINK).build()))
                .isInstanceOf(LinkCodeNotFoundException.class);
    }
}
