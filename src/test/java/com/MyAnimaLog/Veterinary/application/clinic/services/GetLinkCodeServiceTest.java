package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetLinkCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLinkCodeServiceTest {

    private static final String LINK = "https://app.myanimalog.com/clinicas/vincular?code=ABCD2345";

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private GetLinkCodeService getLinkCodeService;

    private UUID veterinaryId;
    private Veterinary veterinary;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        veterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .inviteCode("ABCD2345")
                .inviteLink(LINK)
                .build();
    }

    @Test
    void getLinkCode_shouldReturnTheExistingCode() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));

        GetLinkCodeResponse response = getLinkCodeService.getLinkCode(veterinaryId);

        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getInviteCode()).isEqualTo("ABCD2345");
        assertThat(response.getInviteLink()).isEqualTo(LINK);
        assertThat(response.isHasCode()).isTrue();
    }

    @Test
    void getLinkCode_shouldNeverRegenerate() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));

        getLinkCodeService.getLinkCode(veterinaryId);

        // Si esto generara el código, abrir la pantalla invalidaría los códigos ya repartidos.
        verify(veterinaryRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any());
        verify(veterinaryRepositoryPort, never())
                .existsByInviteCode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void getLinkCode_shouldReportNoCode_whenClinicNeverGeneratedOne() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                veterinary.toBuilder().inviteCode(null).inviteLink(null).build()));

        GetLinkCodeResponse response = getLinkCodeService.getLinkCode(veterinaryId);

        assertThat(response.getInviteCode()).isNull();
        assertThat(response.getInviteLink()).isNull();
        assertThat(response.isHasCode()).isFalse();
    }

    @Test
    void getLinkCode_shouldStillWork_whenClinicIsSuspended() {
        // A diferencia de generar, mostrar el código que la clínica ya tiene no exige ACTIVE:
        // esconderlo solo la confundiría (vincularse con él sí se rechaza).
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                veterinary.toBuilder().status(VeterinaryStatus.SUSPENDED).active(false).build()));

        assertThat(getLinkCodeService.getLinkCode(veterinaryId).getInviteCode()).isEqualTo("ABCD2345");
    }

    @Test
    void getLinkCode_shouldThrowVeterinaryNotFound_whenClinicDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getLinkCodeService.getLinkCode(veterinaryId))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void getLinkCode_shouldRequireClinicConfigure() {
        when(authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE))
                .thenThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE));

        assertThatThrownBy(() -> getLinkCodeService.getLinkCode(veterinaryId))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(veterinaryRepositoryPort, never()).findById(veterinaryId);
    }

    @Test
    void getLinkCode_shouldThrowTenantMismatch_whenCallerBelongsToAnotherClinic() {
        when(authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE))
                .thenThrow(new TenantMismatchException());

        assertThatThrownBy(() -> getLinkCodeService.getLinkCode(veterinaryId))
                .isInstanceOf(TenantMismatchException.class);
    }
}
