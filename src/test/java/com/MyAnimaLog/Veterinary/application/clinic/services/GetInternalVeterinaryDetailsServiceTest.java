package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetInternalVeterinaryDetailsResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInternalVeterinaryDetailsServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @InjectMocks
    private GetInternalVeterinaryDetailsService getInternalVeterinaryDetailsService;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    @Test
    void get_shouldReturnTheExactFieldsCalendarExpects() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .legalName("El Bosque SAS")
                .nit(Nit.fromPersisted("900123456-1"))
                .legalRepName("Ana Pérez")
                .email("elbosque@correo.com")
                .phone("3001234567")
                .address("Calle 1 #2-3")
                .timezone("America/Bogota")
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .allowOnlineBooking(true)
                .bookingRequiresConfirmation(true)
                .cancellationMinHours(4)
                .tenantId(UUID.randomUUID().toString())
                .build()));

        GetInternalVeterinaryDetailsResponse response = getInternalVeterinaryDetailsService.get(veterinaryId);

        assertThat(response.getId()).isEqualTo(veterinaryId);
        assertThat(response.getName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(response.getAllowOnlineBooking()).isTrue();
        assertThat(response.getBookingRequiresConfirmation()).isTrue();
        assertThat(response.getCancellationMinHours()).isEqualTo(4);
        assertThat(response.getTimezone()).isEqualTo("America/Bogota");
        assertThat(response.getAddress()).isEqualTo("Calle 1 #2-3");
        assertThat(response.getPhone()).isEqualTo("3001234567");
    }

    @Test
    void get_shouldReturnTheRealSixStatusValues() {
        for (VeterinaryStatus status : VeterinaryStatus.values()) {
            when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                    .id(veterinaryId).name("Clínica").city("Bogotá")
                    .tenantId(UUID.randomUUID().toString()).status(status).active(true).build()));

            assertThat(getInternalVeterinaryDetailsService.get(veterinaryId).getStatus()).isEqualTo(status);
        }
    }

    @Test
    void get_shouldAllowNullAddressAndPhone() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                .id(veterinaryId).name("Clínica").city("Bogotá")
                .tenantId(UUID.randomUUID().toString()).status(VeterinaryStatus.ACTIVE).active(true)
                .address(null).phone(null).build()));

        GetInternalVeterinaryDetailsResponse response = getInternalVeterinaryDetailsService.get(veterinaryId);

        assertThat(response.getAddress()).isNull();
        assertThat(response.getPhone()).isNull();
    }

    @Test
    void get_shouldThrowVeterinaryNotFound_whenTheClinicDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getInternalVeterinaryDetailsService.get(veterinaryId))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    /**
     * NIT, razón social, representante legal y email no tienen campo en la respuesta: este test
     * existe para que agregarlos no pase inadvertido (mismo patrón que
     * {@code PreviewVeterinaryLinkServiceTest.preview_shouldNotExposeInternalClinicData}).
     */
    @Test
    void response_shouldNotExposeTaxOrPersonalData() throws NoSuchFieldException {
        for (String forbidden : new String[]{"nit", "legalName", "legalRepName", "email"}) {
            assertThatThrownBy(() -> GetInternalVeterinaryDetailsResponse.class.getDeclaredField(forbidden))
                    .isInstanceOf(NoSuchFieldException.class);
        }
        Field[] fields = GetInternalVeterinaryDetailsResponse.class.getDeclaredFields();
        assertThat(fields).extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "name", "status", "allowOnlineBooking",
                        "bookingRequiresConfirmation", "cancellationMinHours", "timezone", "address", "phone");
    }
}
