package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.infrastructure.security.InvitationTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetInvitationPreviewServiceTest {

    private static final String TOKEN = "token-de-prueba";

    @Mock
    private EmployeeInvitationRepositoryPort invitationRepositoryPort;

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private UserServicePort userServicePort;

    @InjectMocks
    private GetInvitationPreviewService getInvitationPreviewService;

    private UUID veterinaryId;
    private UUID invitedBy;
    private EmployeeInvitation invitation;
    private GetInvitationPreviewRequest request;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        invitedBy = UUID.randomUUID();

        invitation = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email("invitado@clinica.com")
                .role(EmployeeRole.RECEPTIONIST)
                .tokenHash(InvitationTokens.hash(TOKEN))
                .status(InvitationStatus.PENDING)
                .invitedBy(invitedBy)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .build();
        request = GetInvitationPreviewRequest.builder().token(TOKEN).build();

        when(invitationRepositoryPort.findByTokenHash(InvitationTokens.hash(TOKEN)))
                .thenReturn(Optional.of(invitation));
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Bogotá")
                .logoUrl("https://cdn/logo.png")
                .status(VeterinaryStatus.ACTIVE)
                .build()));
        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(
                UUID.randomUUID(), "invitado@clinica.com", true, false, Optional.empty()));
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of(
                invitedBy, new UserServicePort.UserInfo(invitedBy, "owner@clinica.com", true, "Ana Dueña")));
    }

    @Test
    void preview_shouldShowTheClinicRoleAndInviterForAUsableInvitation() {
        GetInvitationPreviewResponse response = getInvitationPreviewService.preview(request);

        assertThat(response.getVeterinaryName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getVeterinaryCity()).isEqualTo("Bogotá");
        assertThat(response.getLogoUrl()).isEqualTo("https://cdn/logo.png");
        assertThat(response.getRole()).isEqualTo(EmployeeRole.RECEPTIONIST);
        assertThat(response.getInvitedByName()).isEqualTo("Ana Dueña");
        assertThat(response.getExpiresAt()).isEqualTo(invitation.getExpiresAt());
        assertThat(response.isEmailMatchesCaller()).isTrue();
        assertThat(response.isExpired()).isFalse();
        assertThat(response.isUsable()).isTrue();
    }

    @Test
    void preview_shouldNotBeUsable_whenTheSessionBelongsToAnotherEmail() {
        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(
                UUID.randomUUID(), "otra@persona.com", true, false, Optional.empty()));

        GetInvitationPreviewResponse response = getInvitationPreviewService.preview(request);

        // 200 con el estado, no un error: el frontend avisa "iniciaste sesión con otra cuenta".
        assertThat(response.isEmailMatchesCaller()).isFalse();
        assertThat(response.isUsable()).isFalse();
        assertThat(response.getEmail()).isEqualTo("invitado@clinica.com");
    }

    @Test
    void preview_shouldNotBeUsable_whenTheCallerEmailIsNotVerified() {
        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(
                UUID.randomUUID(), "invitado@clinica.com", false, false, Optional.empty()));

        GetInvitationPreviewResponse response = getInvitationPreviewService.preview(request);

        assertThat(response.isEmailMatchesCaller()).isTrue();
        assertThat(response.isUsable()).isFalse();
    }

    @Test
    void preview_shouldReportExpiredWithoutFailing() {
        when(invitationRepositoryPort.findByTokenHash(InvitationTokens.hash(TOKEN)))
                .thenReturn(Optional.of(invitation.toBuilder()
                        .expiresAt(LocalDateTime.now().minusDays(1)).build()));

        GetInvitationPreviewResponse response = getInvitationPreviewService.preview(request);

        assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(response.isExpired()).isTrue();
        assertThat(response.isUsable()).isFalse();
    }

    @Test
    void preview_shouldReportRevokedWithoutFailing() {
        when(invitationRepositoryPort.findByTokenHash(InvitationTokens.hash(TOKEN)))
                .thenReturn(Optional.of(invitation.toBuilder()
                        .status(InvitationStatus.REVOKED).build()));

        GetInvitationPreviewResponse response = getInvitationPreviewService.preview(request);

        assertThat(response.getStatus()).isEqualTo(InvitationStatus.REVOKED);
        assertThat(response.isUsable()).isFalse();
    }

    @Test
    void preview_shouldThrowInvitationNotFoundException_whenTheTokenIsUnknownOrMissing() {
        when(invitationRepositoryPort.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getInvitationPreviewService.preview(request))
                .isInstanceOf(InvitationNotFoundException.class);

        assertThatThrownBy(() -> getInvitationPreviewService.preview(
                GetInvitationPreviewRequest.builder().token(null).build()))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void preview_shouldStillAnswer_whenTheInviterNameCannotBeResolved() {
        when(userServicePort.getUsers(List.of(invitedBy))).thenReturn(Map.of());

        GetInvitationPreviewResponse response = getInvitationPreviewService.preview(request);

        assertThat(response.getInvitedByName()).isNull();
        assertThat(response.isUsable()).isTrue();
    }
}
