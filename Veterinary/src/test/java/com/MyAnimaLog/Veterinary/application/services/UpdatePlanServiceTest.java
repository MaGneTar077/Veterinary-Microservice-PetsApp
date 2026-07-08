package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.UpdatePlanRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdatePlanResponse;
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
class UpdatePlanServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;

    @InjectMocks
    private UpdatePlanService updatePlanService;

    private UUID veterinaryId;
    private Veterinary veterinary;
    private VeterinarySubscription activeSubscription;
    private VeterinarySubscription updatedSubscription;

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
                .plan("PRO")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2027, 7, 1))
                .active(true)
                .build();

        updatedSubscription = activeSubscription.toBuilder()
                .plan("ENTERPRISE")
                .endDate(LocalDate.of(2028, 7, 1))
                .build();
    }

    @Test
    void updatePlan_shouldReturnResponse_whenRequestIsValid() {
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .plan("ENTERPRISE")
                .endDate(LocalDate.of(2028, 7, 1))
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId))
                .thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepositoryPort.save(any(VeterinarySubscription.class)))
                .thenReturn(updatedSubscription);

        UpdatePlanResponse response = updatePlanService.updatePlan(veterinaryId, request);

        assertThat(response).isNotNull();
        assertThat(response.getPlan()).isEqualTo("ENTERPRISE");
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2028, 7, 1));
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void updatePlan_shouldKeepExistingValues_whenFieldsAreNull() {
        UpdatePlanRequest request = UpdatePlanRequest.builder().build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId))
                .thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepositoryPort.save(any(VeterinarySubscription.class)))
                .thenReturn(activeSubscription);

        updatePlanService.updatePlan(veterinaryId, request);

        verify(subscriptionRepositoryPort, times(1)).save(argThat(s ->
                s.getPlan().equals("PRO") &&
                        s.getEndDate().equals(LocalDate.of(2027, 7, 1))
        ));
    }

    @Test
    void updatePlan_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                updatePlanService.updatePlan(veterinaryId, UpdatePlanRequest.builder().build())
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void updatePlan_shouldThrowIllegalArgumentException_whenEndDateIsBeforeStartDate() {
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2026, 1, 1))
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));

        assertThatThrownBy(() ->
                updatePlanService.updatePlan(veterinaryId, request)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date cannot be before start date");
    }

    @Test
    void updatePlan_shouldThrowNoActiveSubscriptionException_whenNoActiveSubscription() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                updatePlanService.updatePlan(veterinaryId, UpdatePlanRequest.builder().build())
        ).isInstanceOf(NoActiveSubscriptionException.class);
    }

    @Test
    void updatePlan_shouldCallSave_once() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId))
                .thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepositoryPort.save(any(VeterinarySubscription.class)))
                .thenReturn(updatedSubscription);

        updatePlanService.updatePlan(veterinaryId, UpdatePlanRequest.builder().build());

        verify(subscriptionRepositoryPort, times(1)).save(any(VeterinarySubscription.class));
    }

    @Test
    void updatePlan_shouldNeverCallSave_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                updatePlanService.updatePlan(veterinaryId, UpdatePlanRequest.builder().build())
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(subscriptionRepositoryPort, never()).save(any());
    }

    @Test
    void updatePlan_shouldNeverCallSave_whenNoActiveSubscription() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                updatePlanService.updatePlan(veterinaryId, UpdatePlanRequest.builder().build())
        ).isInstanceOf(NoActiveSubscriptionException.class);

        verify(subscriptionRepositoryPort, never()).save(any());
    }
}