package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GenerateInviteCodeRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.GenerateInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateInviteCodeServiceTest {

    private static final String LINK_BASE_URL = "https://app.myanimalog.com/clinicas/vincular";

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private GenerateInviteCodeService generateInviteCodeService;

    private UUID veterinaryId;
    private Veterinary veterinary;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(generateInviteCodeService, "linkBaseUrl", LINK_BASE_URL);

        veterinaryId = UUID.randomUUID();

        veterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /** Devuelve lo que el servicio mandó a guardar, que es donde está el código recién generado. */
    private GenerateInviteCodeResponse generateEchoingSave() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(veterinaryRepositoryPort.existsByInviteCode(anyString())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        return generateInviteCodeService.generateInviteCode(
                GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build());
    }

    @Test
    void generateInviteCode_shouldReturnResponse_whenVeterinaryIsActive() {
        GenerateInviteCodeResponse response = generateEchoingSave();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(veterinaryId);
        assertThat(response.getInviteCode()).isNotBlank();
        assertThat(response.getInviteLink()).isNotBlank();
    }

    @Test
    void generateInviteCode_shouldUseEightCharactersWithoutAmbiguousOnes() {
        GenerateInviteCodeResponse response = generateEchoingSave();

        // 8 caracteres, sin el prefijo VET- que usaba la Fase 2, y sin I/L/O/0/1: el código se
        // dicta por teléfono y se copia de un mostrador.
        assertThat(response.getInviteCode())
                .hasSize(8)
                .doesNotStartWith("VET-")
                .matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}");
    }

    @Test
    void generateInviteCode_inviteLink_shouldPointToTheFrontendScreen() {
        GenerateInviteCodeResponse response = generateEchoingSave();

        assertThat(response.getInviteLink())
                .isEqualTo(LINK_BASE_URL + "?code=" + response.getInviteCode());
    }

    @Test
    void generateInviteCode_inviteLink_shouldUseAmpersand_whenBaseUrlAlreadyHasQueryString() {
        ReflectionTestUtils.setField(generateInviteCodeService, "linkBaseUrl",
                "https://app.myanimalog.com/vincular?utm=qr");

        GenerateInviteCodeResponse response = generateEchoingSave();

        assertThat(response.getInviteLink())
                .isEqualTo("https://app.myanimalog.com/vincular?utm=qr&code=" + response.getInviteCode());
    }

    @Test
    void generateInviteCode_shouldThrowClinicNotActiveException_whenClinicIsNotActive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                veterinary.toBuilder().status(VeterinaryStatus.PENDING_DOCUMENTS).active(false).build()));

        assertThatThrownBy(() ->
                generateInviteCodeService.generateInviteCode(
                        GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build())
        ).isInstanceOf(ClinicNotActiveException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void generateInviteCode_shouldThrowClinicNotActiveException_whenClinicIsSuspended() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                veterinary.toBuilder().status(VeterinaryStatus.SUSPENDED).active(false).build()));

        assertThatThrownBy(() ->
                generateInviteCodeService.generateInviteCode(
                        GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build())
        ).isInstanceOf(ClinicNotActiveException.class);
    }

    @Test
    void generateInviteCode_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                generateInviteCodeService.generateInviteCode(
                        GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build())
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void generateInviteCode_shouldRetryCode_whenCodeAlreadyExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(veterinaryRepositoryPort.existsByInviteCode(anyString()))
                .thenReturn(true)   // primera generación colisiona
                .thenReturn(false); // segunda es única
        when(veterinaryRepositoryPort.save(any(Veterinary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        generateInviteCodeService.generateInviteCode(
                GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build());

        verify(veterinaryRepositoryPort, times(2)).existsByInviteCode(anyString());
    }

    @Test
    void generateInviteCode_shouldOverwriteExistingCode_whenVeterinaryAlreadyHasOne() {
        Veterinary withOldCode = veterinary.toBuilder()
                .inviteCode("ABCD2345")
                .inviteLink(LINK_BASE_URL + "?code=ABCD2345")
                .build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(withOldCode));
        when(veterinaryRepositoryPort.existsByInviteCode(anyString())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GenerateInviteCodeResponse response = generateInviteCodeService.generateInviteCode(
                GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build());

        // Regenerar invalida el anterior: es la única forma de cortar un código que se filtró.
        ArgumentCaptor<Veterinary> saved = ArgumentCaptor.forClass(Veterinary.class);
        verify(veterinaryRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getInviteCode()).isNotEqualTo("ABCD2345");
        assertThat(response.getInviteCode()).isNotEqualTo("ABCD2345");
    }

    @Test
    void generateInviteCode_shouldCallSave_once() {
        generateEchoingSave();

        verify(veterinaryRepositoryPort, times(1)).save(any(Veterinary.class));
    }

    @Test
    void generateInviteCode_shouldThrowInsufficientPermissionException_whenCallerLacksClinicConfigure() {
        when(authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE))
                .thenThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE));

        assertThatThrownBy(() ->
                generateInviteCodeService.generateInviteCode(
                        GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build())
        ).isInstanceOf(InsufficientPermissionException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void generateInviteCode_shouldThrowTenantMismatchException_whenCallerBelongsToAnotherVeterinary() {
        when(authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE))
                .thenThrow(new TenantMismatchException());

        assertThatThrownBy(() ->
                generateInviteCodeService.generateInviteCode(
                        GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build())
        ).isInstanceOf(TenantMismatchException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}
