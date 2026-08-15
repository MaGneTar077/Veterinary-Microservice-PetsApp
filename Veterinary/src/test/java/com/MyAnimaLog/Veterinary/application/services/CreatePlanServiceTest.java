package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.CreatePlanRequest;
import com.MyAnimaLog.Veterinary.application.dto.CreatePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.ActiveSubscriptionAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotActiveException;
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
class CreatePlanServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;

    @InjectMocks
    private CreatePlanService createPlanService;

    private UUID veterinaryId;
    private Veterinary activeVeterinary;
    private Veterinary inactiveVeterinary;
    private CreatePlanRequest validRequest;
    private VeterinarySubscription savedSubscription;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        activeVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        inactiveVeterinary = activeVeterinary.toBuilder()
                .active(false)
                .build();

        validRequest = CreatePlanRequest.builder()
                .veterinaryId(veterinaryId)
                .plan("PRO")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2027, 7, 1))
                .build();

        savedSubscription = VeterinarySubscription.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .plan("PRO")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2027, 7, 1))
                .active(true)
                .build();
    }

    @Test
    void createPlan_shouldReturnResponse_whenRequestIsValid() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(false);
        when(subscriptionRepositoryPort.save(any(VeterinarySubscription.class))).thenReturn(savedSubscription);

        CreatePlanResponse response = createPlanService.createPlan(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getPlan()).isEqualTo("PRO");
        assertThat(response.getActive()).isTrue();
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2027, 7, 1));
    }

    @Test
    void createPlan_shouldThrowInvalidVeterinaryNameException_whenPlanIsNull() {
        validRequest.setPlan(null);

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(InvalidVeterinaryNameException.class);
    }

    @Test
    void createPlan_shouldThrowInvalidVeterinaryNameException_whenPlanIsBlank() {
        validRequest.setPlan("   ");

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(InvalidVeterinaryNameException.class);
    }

    @Test
    void createPlan_shouldThrowIllegalArgumentException_whenStartDateIsNull() {
        validRequest.setStartDate(null);

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date and end date are required");
    }

    @Test
    void createPlan_shouldThrowIllegalArgumentException_whenEndDateIsNull() {
        validRequest.setEndDate(null);

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date and end date are required");
    }

    @Test
    void createPlan_shouldThrowIllegalArgumentException_whenEndDateIsBeforeStartDate() {
        validRequest.setEndDate(LocalDate.of(2025, 1, 1));

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date cannot be before start date");
    }

    @Test
    void createPlan_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void createPlan_shouldThrowVeterinaryNotActiveException_whenVeterinaryIsInactive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(inactiveVeterinary));

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(VeterinaryNotActiveException.class);
    }

    @Test
    void createPlan_shouldThrowActiveSubscriptionAlreadyExistsException_whenActiveSubExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(true);

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(ActiveSubscriptionAlreadyExistsException.class);
    }

    @Test
    void createPlan_shouldCallSave_once() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(false);
        when(subscriptionRepositoryPort.save(any(VeterinarySubscription.class))).thenReturn(savedSubscription);

        createPlanService.createPlan(validRequest);

        verify(subscriptionRepositoryPort, times(1)).save(any(VeterinarySubscription.class));
    }

    @Test
    void createPlan_shouldNeverCallSave_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(subscriptionRepositoryPort, never()).save(any());
    }

    @Test
    void createPlan_shouldNeverCallSave_whenActiveSubscriptionExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(true);

        assertThatThrownBy(() ->
                createPlanService.createPlan(validRequest)
        ).isInstanceOf(ActiveSubscriptionAlreadyExistsException.class);

        verify(subscriptionRepositoryPort, never()).save(any());
    }
}