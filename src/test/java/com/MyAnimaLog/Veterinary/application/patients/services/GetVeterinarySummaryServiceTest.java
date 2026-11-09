package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinarySummaryResponse;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVeterinarySummaryServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @InjectMocks
    private GetVeterinarySummaryService getVeterinarySummaryService;

    @Test
    void getSummary_shouldReturn200_whenClinicExists() {
        UUID veterinaryId = UUID.randomUUID();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                .id(veterinaryId).name("Clínica El Bosque").timezone("America/Bogota")
                .status(VeterinaryStatus.ACTIVE).logoUrl("https://cdn/logo.png").build()));

        GetVeterinarySummaryResponse response = getVeterinarySummaryService.getSummary(veterinaryId);

        assertThat(response.getName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getTimezone()).isEqualTo("America/Bogota");
        assertThat(response.getStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
    }

    @Test
    void getSummary_shouldThrow404_whenClinicDoesNotExist() {
        UUID veterinaryId = UUID.randomUUID();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getVeterinarySummaryService.getSummary(veterinaryId))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }
}
