package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.dto.GetMyInvitationsResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetMyInvitationsServiceTest {

    @Mock
    private EmployeeInvitationRepositoryPort invitationRepositoryPort;

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private GetMyInvitationsService getMyInvitationsService;

    private UUID veterinaryId;
    private EmployeeInvitation pending;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        pending = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email("invitado@clinica.com")
                .role(EmployeeRole.VETERINARIAN)
                .tokenHash("no-debe-salir")
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(
                UUID.randomUUID(), "Invitado@Clinica.com", true, false, Optional.empty()));
        when(invitationRepositoryPort.findByEmailAndStatus("invitado@clinica.com", InvitationStatus.PENDING))
                .thenReturn(List.of(pending));
        when(veterinaryRepositoryPort.findByIdIn(List.of(veterinaryId))).thenReturn(List.of(Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Bogotá")
                .logoUrl("https://cdn/logo.png")
                .build()));
    }

    @Test
    void myInvitations_shouldLookUpByTheNormalizedEmailFromTheToken() {
        GetMyInvitationsResponse response = getMyInvitationsService.myInvitations();

        verify(invitationRepositoryPort).findByEmailAndStatus("invitado@clinica.com", InvitationStatus.PENDING);
        assertThat(response.getInvitations()).hasSize(1);
        assertThat(response.getInvitations().get(0).getVeterinaryName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getInvitations().get(0).getVeterinaryCity()).isEqualTo("Bogotá");
        assertThat(response.getInvitations().get(0).getLogoUrl()).isEqualTo("https://cdn/logo.png");
        assertThat(response.getInvitations().get(0).getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
    }

    @Test
    void myInvitations_shouldNeverExposeAnyTokenData() {
        GetMyInvitationsResponse response = getMyInvitationsService.myInvitations();

        assertThat(response.getInvitations().get(0).toString()).doesNotContain("no-debe-salir");
    }

    @Test
    void myInvitations_shouldHideExpiredInvitations() {
        when(invitationRepositoryPort.findByEmailAndStatus("invitado@clinica.com", InvitationStatus.PENDING))
                .thenReturn(List.of(pending.toBuilder()
                        .expiresAt(LocalDateTime.now().minusMinutes(1)).build()));

        assertThat(getMyInvitationsService.myInvitations().getInvitations()).isEmpty();
    }

    @Test
    void myInvitations_shouldResolveClinicsInASingleBatchQuery() {
        UUID otherClinicId = UUID.randomUUID();
        when(invitationRepositoryPort.findByEmailAndStatus("invitado@clinica.com", InvitationStatus.PENDING))
                .thenReturn(List.of(pending, pending.toBuilder()
                        .id(UUID.randomUUID()).veterinaryId(otherClinicId).build()));
        when(veterinaryRepositoryPort.findByIdIn(List.of(veterinaryId, otherClinicId)))
                .thenReturn(List.of(Veterinary.builder().id(veterinaryId).name("Clínica El Bosque").build()));

        GetMyInvitationsResponse response = getMyInvitationsService.myInvitations();

        assertThat(response.getInvitations()).hasSize(2);
        verify(veterinaryRepositoryPort).findByIdIn(List.of(veterinaryId, otherClinicId));
        // Una clínica que no se pudo resolver no rompe la lista: la invitación sigue apareciendo.
        assertThat(response.getInvitations().get(1).getVeterinaryName()).isNull();
        assertThat(response.getInvitations().get(1).getVeterinaryId()).isEqualTo(otherClinicId);
    }

    @Test
    void myInvitations_shouldReturnEmpty_whenTheTokenCarriesNoEmail() {
        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(
                UUID.randomUUID(), null, true, false, Optional.empty()));

        assertThat(getMyInvitationsService.myInvitations().getInvitations()).isEmpty();

        verify(invitationRepositoryPort, never()).findByEmailAndStatus(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void myInvitations_shouldNotQueryClinics_whenThereAreNoPendingInvitations() {
        when(invitationRepositoryPort.findByEmailAndStatus("invitado@clinica.com", InvitationStatus.PENDING))
                .thenReturn(List.of());

        assertThat(getMyInvitationsService.myInvitations().getInvitations()).isEmpty();

        verify(veterinaryRepositoryPort, never()).findByIdIn(anyList());
    }
}
