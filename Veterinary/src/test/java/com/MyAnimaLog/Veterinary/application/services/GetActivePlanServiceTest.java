package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.GetActivePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.NoActiveSubscriptionException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.model.VeterinarySubscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetActivePlanServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;

    @InjectMocks
    private GetActivePlanService getActivePlanService;

    private UUID veterinaryId;
    private Veterinary veterinary;
    private VeterinarySubscription activeSubscription;
    private VeterinarySubscription expiredSubscription;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        veterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        activeSubscription = VeterinarySubscription.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .plan("BASIC")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .active(true)
                .build();

        expiredSubscription = VeterinarySubscription.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .plan("BASIC")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 6, 30))
                .active(true)
                .build();
    }

    @Test
    void getActivePlan_shouldReturnResponse_whenSubscriptionIsActive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId))
                .thenReturn(Optional.of(activeSubscription));

        GetActivePlanResponse response = getActivePlanService.getActivePlan(veterinaryId);

        assertThat(response).isNotNull();
        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getPlan()).isEqualTo("BASIC");
        assertThat(response.getActive()).isTrue();
        assertThat(response.getEndDate()).isAfterOrEqualTo(LocalDate.now());
    }

    @Test
    void getActivePlan_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                getActivePlanService.getActivePlan(veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void getActivePlan_shouldThrowNoActiveSubscriptionException_whenNoSubscriptionFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                getActivePlanService.getActivePlan(veterinaryId)
        ).isInstanceOf(NoActiveSubscriptionException.class);
    }

    @Test
    void getActivePlan_shouldThrowNoActiveSubscriptionException_whenSubscriptionIsExpired() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId))
                .thenReturn(Optional.of(expiredSubscription));

        assertThatThrownBy(() ->
                getActivePlanService.getActivePlan(veterinaryId)
        ).isInstanceOf(NoActiveSubscriptionException.class)
                .hasMessage("Subscription has expired");
    }

    @Test
    void getActivePlan_shouldNeverCallSubscriptionPort_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                getActivePlanService.getActivePlan(veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(subscriptionRepositoryPort, never()).findActiveByVeterinaryId(any());
    }

    @Test
    void getActivePlan_shouldReturnCorrectPlanDetails() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId))
                .thenReturn(Optional.of(activeSubscription));

        GetActivePlanResponse response = getActivePlanService.getActivePlan(veterinaryId);

        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(response.getId()).isEqualTo(activeSubscription.getId());
    }
}