package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.IsExpiredResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.SubscriptionNotFoundException;
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
class IsExpiredServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;

    @InjectMocks
    private IsExpiredService isExpiredService;

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
                .active(false)
                .build();
    }

    @Test
    void isExpired_shouldReturnExpiredFalse_whenSubscriptionIsActive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findLatestByVeterinaryId(veterinaryId))
                .thenReturn(Optional.of(activeSubscription));

        IsExpiredResponse response = isExpiredService.isExpired(veterinaryId);

        assertThat(response).isNotNull();
        assertThat(response.getExpired()).isFalse();
        assertThat(response.getPlan()).isEqualTo("BASIC");
        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
    }

    @Test
    void isExpired_shouldReturnExpiredTrue_whenSubscriptionIsExpired() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findLatestByVeterinaryId(veterinaryId))
                .thenReturn(Optional.of(expiredSubscription));

        IsExpiredResponse response = isExpiredService.isExpired(veterinaryId);

        assertThat(response).isNotNull();
        assertThat(response.getExpired()).isTrue();
        assertThat(response.getEndDate()).isBefore(LocalDate.now());
    }

    @Test
    void isExpired_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                isExpiredService.isExpired(veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void isExpired_shouldThrowSubscriptionNotFoundException_whenNoSubscriptionFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findLatestByVeterinaryId(veterinaryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                isExpiredService.isExpired(veterinaryId)
        ).isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void isExpired_shouldNeverCallSubscriptionPort_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                isExpiredService.isExpired(veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(subscriptionRepositoryPort, never()).findLatestByVeterinaryId(any());
    }

    @Test
    void isExpired_shouldReturnCorrectEndDate() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(subscriptionRepositoryPort.findLatestByVeterinaryId(veterinaryId))
                .thenReturn(Optional.of(activeSubscription));

        IsExpiredResponse response = isExpiredService.isExpired(veterinaryId);

        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }
}