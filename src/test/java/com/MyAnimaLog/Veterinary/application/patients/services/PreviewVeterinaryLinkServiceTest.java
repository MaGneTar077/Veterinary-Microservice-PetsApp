package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.LinkCodeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviewVeterinaryLinkServiceTest {

    private static final String CODE = "ABCD2345";

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private PreviewVeterinaryLinkService previewVeterinaryLinkService;

    private UUID userId;
    private UUID veterinaryId;
    private Veterinary clinic;
    private PreviewVeterinaryLinkRequest request;

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
                .department("Bolívar")
                .address("Calle 1 #2-3")
                .logoUrl("https://cdn.test/logo.png")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .legalName("El Bosque SAS")
                .nit(Nit.fromPersisted("900123456-1"))
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .inviteCode(CODE)
                .build();

        request = PreviewVeterinaryLinkRequest.builder().code(CODE).build();
    }

    @Test
    void preview_shouldReturnThePublicFacingClinicData() {
        when(veterinaryRepositoryPort.findByInviteCode(CODE)).thenReturn(Optional.of(clinic));
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());

        PreviewVeterinaryLinkResponse response = previewVeterinaryLinkService.preview(request);

        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getLogoUrl()).isEqualTo("https://cdn.test/logo.png");
        assertThat(response.getCity()).isEqualTo("Cartagena");
        assertThat(response.getDepartment()).isEqualTo("Bolívar");
        assertThat(response.getAddress()).isEqualTo("Calle 1 #2-3");
        assertThat(response.isAlreadyLinked()).isFalse();
    }

    @Test
    void preview_shouldNotExposeInternalClinicData() {
        when(veterinaryRepositoryPort.findByInviteCode(CODE)).thenReturn(Optional.of(clinic));
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());

        previewVeterinaryLinkService.preview(request);

        // Cualquiera con un código puede llamar a esto, así que la respuesta no puede traer NIT,
        // razón social, estado ni el contacto interno de la clínica. El DTO no tiene esos campos:
        // este test existe para que agregarlos no pase inadvertido.
        assertThat(PreviewVeterinaryLinkResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("veterinaryId", "name", "logoUrl", "city", "department",
                        "address", "alreadyLinked");
    }

    @Test
    void preview_shouldReportAlreadyLinked_whenTheUserIsLinked() {
        when(veterinaryRepositoryPort.findByInviteCode(CODE)).thenReturn(Optional.of(clinic));
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.of(
                UserVeterinaryLink.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .veterinaryId(veterinaryId)
                        .status(LinkStatus.LINKED)
                        .build()));

        assertThat(previewVeterinaryLinkService.preview(request).isAlreadyLinked()).isTrue();
    }

    @Test
    void preview_shouldThrowLinkCodeNotFound_whenCodeDoesNotExist() {
        when(veterinaryRepositoryPort.findByInviteCode(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> previewVeterinaryLinkService.preview(request))
                .isInstanceOf(LinkCodeNotFoundException.class);
    }

    @Test
    void preview_shouldThrowLinkCodeNotFound_whenClinicIsNotActive() {
        when(veterinaryRepositoryPort.findByInviteCode(CODE)).thenReturn(Optional.of(
                clinic.toBuilder().status(VeterinaryStatus.PENDING_DOCUMENTS).active(false).build()));

        assertThatThrownBy(() -> previewVeterinaryLinkService.preview(request))
                .isInstanceOf(LinkCodeNotFoundException.class);
    }

    @Test
    void preview_shouldThrowInvalidInviteCode_whenCodeIsMissing() {
        assertThatThrownBy(() -> previewVeterinaryLinkService.preview(
                PreviewVeterinaryLinkRequest.builder().code(null).build()))
                .isInstanceOf(InvalidInviteCodeException.class);
    }
}
