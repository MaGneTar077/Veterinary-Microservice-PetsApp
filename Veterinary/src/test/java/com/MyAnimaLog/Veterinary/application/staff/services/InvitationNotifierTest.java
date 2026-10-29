package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvitationNotifierTest {

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private InvitationNotifier invitationNotifier;

    private Veterinary veterinary;
    private EmployeeInvitation invitation;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitationNotifier, "invitationBaseUrl",
                "https://app.myanimalog.com/invitaciones");

        veterinary = Veterinary.builder()
                .id(UUID.randomUUID())
                .name("Clínica El Bosque")
                .build();

        invitation = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinary.getId())
                .email("nuevo@clinica.com")
                .role(EmployeeRole.VETERINARIAN)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void publishInvited_shouldAddressTheEventToTheInviteeEmailWithTheAcceptLink() {
        invitationNotifier.publishInvited(invitation, "token-en-claro", veterinary);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        DomainEvent event = captor.getValue();

        assertThat(event.eventType()).isEqualTo(VeterinaryEventType.EMPLOYEE_INVITED);
        // El invitado puede no tener cuenta todavía: se identifica por correo, no por userId.
        assertThat(event.userId()).isNull();
        assertThat(event.recipient().email()).isEqualTo("nuevo@clinica.com");
        assertThat(event.payload()).containsEntry("veterinaryName", "Clínica El Bosque");
        assertThat(event.payload()).containsEntry("role", "VETERINARIAN");
        assertThat(event.payload()).containsEntry("acceptUrl",
                "https://app.myanimalog.com/invitaciones?token=token-en-claro");
        assertThat(event.payload()).containsEntry("expiresAt", invitation.getExpiresAt());
        assertThat(event.metadata().veterinaryId()).isEqualTo(veterinary.getId());
    }

    @Test
    void publishInvited_shouldAppendTheTokenWithAmpersand_whenTheBaseUrlAlreadyHasAQueryString() {
        ReflectionTestUtils.setField(invitationNotifier, "invitationBaseUrl",
                "https://app.myanimalog.com/invitaciones?lang=es");

        invitationNotifier.publishInvited(invitation, "abc123", veterinary);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        assertThat(captor.getValue().payload()).containsEntry("acceptUrl",
                "https://app.myanimalog.com/invitaciones?lang=es&token=abc123");
    }
}
