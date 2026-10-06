package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryMemberResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVeterinaryMemberServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;

    @InjectMocks
    private GetVeterinaryMemberService getVeterinaryMemberService;

    private UUID veterinaryId;
    private UUID userId;
    private Veterinary activeVeterinary;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        userId = UUID.randomUUID();

        activeVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getMember_returnsMemberTrueWithRoleAndActive_whenEmployeeIsActive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(true);
        VeterinaryEmployee employee = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .build();
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.of(employee));

        GetVeterinaryMemberResponse response = getVeterinaryMemberService.getMember(veterinaryId, userId);

        assertThat(response.isMember()).isTrue();
        assertThat(response.getEmployeeId()).isEqualTo(employee.getId());
        assertThat(response.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
        assertThat(response.getActive()).isTrue();
        assertThat(response.isLicensed()).isFalse();
        assertThat(response.getVeterinaryStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(response.getSubscriptionStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getMember_returnsMemberTrueWithActiveFalse_whenEmployeeIsInactive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(false);
        VeterinaryEmployee inactiveEmployee = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.ASSISTANT)
                .active(false)
                .build();
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.of(inactiveEmployee));

        GetVeterinaryMemberResponse response = getVeterinaryMemberService.getMember(veterinaryId, userId);

        assertThat(response.isMember()).isTrue();
        assertThat(response.getActive()).isFalse();
        assertThat(response.getSubscriptionStatus()).isEqualTo("NONE");
    }

    @Test
    void getMember_returnsMemberFalseWithNullEmployeeFields_whenUserIsNotAnEmployee() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(true);
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.empty());

        GetVeterinaryMemberResponse response = getVeterinaryMemberService.getMember(veterinaryId, userId);

        assertThat(response.isMember()).isFalse();
        assertThat(response.getEmployeeId()).isNull();
        assertThat(response.getRole()).isNull();
        assertThat(response.getActive()).isNull();
        assertThat(response.isLicensed()).isFalse();
        // La clínica sí existe: status y suscripción se informan igual.
        assertThat(response.getVeterinaryStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(response.getSubscriptionStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getMember_returnsMemberFalseWithEverythingNull_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        GetVeterinaryMemberResponse response = getVeterinaryMemberService.getMember(veterinaryId, userId);

        assertThat(response.isMember()).isFalse();
        assertThat(response.getEmployeeId()).isNull();
        assertThat(response.getRole()).isNull();
        assertThat(response.getActive()).isNull();
        assertThat(response.isLicensed()).isFalse();
        assertThat(response.getVeterinaryStatus()).isNull();
        assertThat(response.getSubscriptionStatus()).isNull();
        verify(employeeRepositoryPort, never()).findByVeterinaryIdAndUserId(any(), any());
        verify(subscriptionRepositoryPort, never()).existsActiveByVeterinaryId(any());
    }

    @Test
    void getMember_returnsSuspendedStatus_whenVeterinaryIsInactive() {
        Veterinary suspendedVeterinary = activeVeterinary.toBuilder().active(false).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(suspendedVeterinary));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(false);
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.empty());

        GetVeterinaryMemberResponse response = getVeterinaryMemberService.getMember(veterinaryId, userId);

        assertThat(response.getVeterinaryStatus()).isEqualTo(VeterinaryStatus.SUSPENDED);
    }
}
