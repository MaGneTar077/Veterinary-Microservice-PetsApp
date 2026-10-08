package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetMyVeterinariesResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyVeterinariesServiceTest {

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @InjectMocks
    private GetMyVeterinariesService getMyVeterinariesService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "owner@example.com", true, false, Optional.empty()));
    }

    @Test
    void getMyVeterinaries_shouldReturnOneEntryPerActiveEmployment_withRoleAndClinicStatus() {
        UUID vet1 = UUID.randomUUID();
        UUID vet2 = UUID.randomUUID();

        VeterinaryEmployee employment1 = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vet1).userId(userId)
                .role(EmployeeRole.OWNER).active(true).build();
        VeterinaryEmployee employment2 = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vet2).userId(userId)
                .role(EmployeeRole.VETERINARIAN).active(true).build();

        when(employeeRepositoryPort.findByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(employment1, employment2));
        when(veterinaryRepositoryPort.findById(vet1)).thenReturn(Optional.of(
                Veterinary.builder().id(vet1).name("Clínica Uno").status(VeterinaryStatus.ACTIVE).build()));
        when(veterinaryRepositoryPort.findById(vet2)).thenReturn(Optional.of(
                Veterinary.builder().id(vet2).name("Clínica Dos").status(VeterinaryStatus.PENDING_DOCUMENTS).build()));

        GetMyVeterinariesResponse response = getMyVeterinariesService.getMyVeterinaries();

        assertThat(response.getVeterinaries()).hasSize(2);
        assertThat(response.getVeterinaries()).anySatisfy(summary -> {
            assertThat(summary.getVeterinaryId()).isEqualTo(vet1);
            assertThat(summary.getName()).isEqualTo("Clínica Uno");
            assertThat(summary.getRole()).isEqualTo(EmployeeRole.OWNER);
            assertThat(summary.getStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        });
        assertThat(response.getVeterinaries()).anySatisfy(summary -> {
            assertThat(summary.getVeterinaryId()).isEqualTo(vet2);
            assertThat(summary.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
            assertThat(summary.getStatus()).isEqualTo(VeterinaryStatus.PENDING_DOCUMENTS);
        });
    }

    @Test
    void getMyVeterinaries_shouldReturnEmptyList_whenCallerHasNoActiveEmployments() {
        when(employeeRepositoryPort.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        GetMyVeterinariesResponse response = getMyVeterinariesService.getMyVeterinaries();

        assertThat(response.getVeterinaries()).isEmpty();
    }

    @Test
    void getMyVeterinaries_shouldSkipEmployment_whenItsVeterinaryNoLongerExists() {
        UUID vet1 = UUID.randomUUID();
        VeterinaryEmployee employment = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vet1).userId(userId)
                .role(EmployeeRole.ASSISTANT).active(true).build();

        when(employeeRepositoryPort.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(employment));
        when(veterinaryRepositoryPort.findById(vet1)).thenReturn(Optional.empty());

        GetMyVeterinariesResponse response = getMyVeterinariesService.getMyVeterinaries();

        assertThat(response.getVeterinaries()).isEmpty();
    }
}
