package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.dto.BusinessHoursResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.BusinessHoursRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidBusinessHoursException;
import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplaceBusinessHoursServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private BusinessHoursRepositoryPort businessHoursRepositoryPort;

    @InjectMocks
    private ReplaceBusinessHoursService replaceBusinessHoursService;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    private void clinicExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                .id(veterinaryId).name("Clínica").city("Bogotá").tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE).active(true).timezone("America/Bogota").build()));
    }

    private ScheduleEntryRequest entry(int day, String opens, String closes) {
        return ScheduleEntryRequest.builder().dayOfWeek(day).opensAt(opens).closesAt(closes).build();
    }

    @Test
    void replace_shouldDeleteTheWholeWeekBeforeInserting() {
        clinicExists();

        BusinessHoursResponse response = replaceBusinessHoursService.replace(veterinaryId,
                List.of(entry(1, "08:00", "17:00"), entry(2, "08:00", "12:00")));

        InOrder inOrder = inOrder(businessHoursRepositoryPort);
        inOrder.verify(businessHoursRepositoryPort).deleteByVeterinaryId(veterinaryId);
        inOrder.verify(businessHoursRepositoryPort).saveAll(anyList());

        ArgumentCaptor<List<BusinessHours>> saved = ArgumentCaptor.forClass(List.class);
        verify(businessHoursRepositoryPort).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(2);
        assertThat(saved.getValue()).allMatch(hours -> veterinaryId.equals(hours.getVeterinaryId()));
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTimezone()).isEqualTo("America/Bogota");
    }

    @Test
    void replace_shouldAcceptAnEmptyWeek_leavingTheClinicWithoutHours() {
        clinicExists();

        BusinessHoursResponse response = replaceBusinessHoursService.replace(veterinaryId, List.of());

        verify(businessHoursRepositoryPort).deleteByVeterinaryId(veterinaryId);
        verify(businessHoursRepositoryPort).saveAll(List.of());
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void replace_shouldRejectOverlappingEntries_withoutTouchingTheCurrentWeek() {
        clinicExists();

        assertThatThrownBy(() -> replaceBusinessHoursService.replace(veterinaryId,
                List.of(entry(1, "08:00", "13:00"), entry(1, "12:00", "17:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);

        verify(businessHoursRepositoryPort, never()).deleteByVeterinaryId(any());
        verify(businessHoursRepositoryPort, never()).saveAll(anyList());
    }

    @Test
    void replace_shouldRejectInvalidEntries_withoutTouchingTheCurrentWeek() {
        clinicExists();

        assertThatThrownBy(() -> replaceBusinessHoursService.replace(veterinaryId,
                List.of(entry(1, "18:00", "09:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
        assertThatThrownBy(() -> replaceBusinessHoursService.replace(veterinaryId,
                List.of(entry(8, "08:00", "09:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
        assertThatThrownBy(() -> replaceBusinessHoursService.replace(veterinaryId,
                List.of(entry(1, "8:00", "09:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);

        verify(businessHoursRepositoryPort, never()).deleteByVeterinaryId(any());
    }

    @Test
    void replace_shouldPropagateInsufficientPermission_whenCallerLacksClinicConfigure() {
        doThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE))
                .when(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);

        assertThatThrownBy(() -> replaceBusinessHoursService.replace(veterinaryId, List.of()))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(businessHoursRepositoryPort, never()).deleteByVeterinaryId(any());
    }

    /**
     * El borrado y la inserción tienen que ir en la misma transacción: si el insert falla y el
     * delete ya se hubiera confirmado, la clínica quedaría sin horario (para Calendar, "no atiende
     * nunca"). Mockito no puede verificar el rollback real, así que se verifica (a) que la excepción
     * se propaga sin quedar a medias en silencio y (b) que el método declara {@code @Transactional},
     * que es lo que hace el rollback.
     */
    @Test
    void replace_shouldBeTransactional_soAFailedInsertKeepsThePreviousWeek() throws Exception {
        clinicExists();
        doThrow(new RuntimeException("insert failed")).when(businessHoursRepositoryPort).saveAll(anyList());

        assertThatThrownBy(() -> replaceBusinessHoursService.replace(veterinaryId,
                List.of(entry(1, "08:00", "17:00"))))
                .isInstanceOf(RuntimeException.class);

        Method replace = ReplaceBusinessHoursService.class.getMethod("replace", UUID.class, List.class);
        assertThat(replace.isAnnotationPresent(Transactional.class)).isTrue();
    }
}
