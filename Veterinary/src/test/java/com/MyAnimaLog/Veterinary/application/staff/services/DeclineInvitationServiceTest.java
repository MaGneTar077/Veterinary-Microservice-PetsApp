package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationExpiredException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeclineInvitationServiceTest {

    private static final String TOKEN = "token-de-prueba";

    @Mock
    private EmployeeInvitationRepositoryPort invitationRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private DeclineInvitationService declineInvitationService;

    private UUID veterinaryId;
    private EmployeeInvitation invitation;
    private DeclineInvitationRequest request;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        invitation = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email("invitado@clinica.com")
                .role(EmployeeRole.ASSISTANT)
                .tokenHash(InvitationTokens.hash(TOKEN))
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .build();
        request = DeclineInvitationRequest.builder().token(TOKEN).build();

        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(
                UUID.randomUUID(), "invitado@clinica.com", true, false, Optional.empty()));
        when(invitationRepositoryPort.findByTokenHash(InvitationTokens.hash(TOKEN)))
                .thenReturn(Optional.of(invitation));
        when(invitationRepositoryPort.save(any(EmployeeInvitation.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void decline_shouldMarkTheInvitationAsDeclined() {
        DeclineInvitationResponse response = declineInvitationService.decline(request);

        assertThat(response.getStatus()).isEqualTo(InvitationStatus.DECLINED);
        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getDecidedAt()).isNotNull();
    }

    @Test
    void decline_shouldThrowInvitationEmailMismatchException_whenTheTokenEmailIsNotTheInvitedOne() {
        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(
                UUID.randomUUID(), "otra@persona.com", true, false, Optional.empty()));

        assertThatThrownBy(() -> declineInvitationService.decline(request))
                .isInstanceOf(InvitationEmailMismatchException.class);

        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void decline_shouldThrowEmailNotVerifiedException_whenTheCallerEmailIsNotVerified() {
        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(
                UUID.randomUUID(), "invitado@clinica.com", false, false, Optional.empty()));

        assertThatThrownBy(() -> declineInvitationService.decline(request))
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    void decline_shouldThrowInvitationNotPendingException_whenItWasAlreadyDecided() {
        when(invitationRepositoryPort.findByTokenHash(InvitationTokens.hash(TOKEN)))
                .thenReturn(Optional.of(invitation.toBuilder().status(InvitationStatus.DECLINED).build()));

        assertThatThrownBy(() -> declineInvitationService.decline(request))
                .isInstanceOf(InvitationNotPendingException.class);
    }

    @Test
    void decline_shouldThrowInvitationExpiredException_whenTheInvitationIsPastItsExpiry() {
        when(invitationRepositoryPort.findByTokenHash(InvitationTokens.hash(TOKEN)))
                .thenReturn(Optional.of(invitation.toBuilder()
                        .expiresAt(LocalDateTime.now().minusMinutes(1)).build()));

        assertThatThrownBy(() -> declineInvitationService.decline(request))
                .isInstanceOf(InvitationExpiredException.class);
    }

    @Test
    void decline_shouldThrowInvitationNotFoundException_whenTheTokenIsUnknown() {
        when(invitationRepositoryPort.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> declineInvitationService.decline(request))
                .isInstanceOf(InvitationNotFoundException.class);
    }
}
