package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidNitException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.TooManyOwnedVeterinariesException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryNitAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryEmailAlreadyExistsException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterVeterinaryServiceTest {

    // Self-consistent with Nit's DIAN check-digit algorithm (not a real-world NIT).
    private static final String VALID_NIT = "123456789-6";

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private RegisterVeterinaryService registerVeterinaryService;

    private UUID callerId;
    private RegisterVeterinaryRequest validRequest;
    private Veterinary savedVeterinary;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(callerId, "caller@example.com", true, false, Optional.empty()));

        validRequest = RegisterVeterinaryRequest.builder()
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .nit(VALID_NIT)
                .build();

        savedVeterinary = Veterinary.builder()
                .id(UUID.randomUUID())
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(false)
                .status(VeterinaryStatus.PENDING_DOCUMENTS)
                .createdAt(LocalDateTime.now())
                .build();

        lenient().when(employeeRepositoryPort.findByUserIdAndRole(callerId, EmployeeRole.OWNER))
                .thenReturn(List.of());
    }

    @Test
    void registerVeterinary_shouldReturnResponse_whenRequestIsValid() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByNit(any())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        RegisterVeterinaryResponse response = registerVeterinaryService.registerVeterinary(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getEmail()).isEqualTo("elbosque@veterinaria.com");
        assertThat(response.getActive()).isFalse();
        assertThat(response.getStatus()).isEqualTo(VeterinaryStatus.PENDING_DOCUMENTS);
        assertThat(response.getTenantId()).isNotNull();
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void registerVeterinary_shouldThrowEmailNotVerifiedException_whenCallerEmailIsNotVerified() {
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(callerId, "caller@example.com", false, false, Optional.empty()));

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(EmailNotVerifiedException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void registerVeterinary_shouldThrowInvalidVeterinaryNameException_whenNameIsNull() {
        validRequest.setName(null);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryNameException.class);
    }

    @Test
    void registerVeterinary_shouldThrowInvalidVeterinaryNameException_whenNameIsBlank() {
        validRequest.setName("   ");

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryNameException.class);
    }

    @Test
    void registerVeterinary_shouldThrowInvalidVeterinaryEmailException_whenEmailIsNull() {
        validRequest.setEmail(null);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryEmailException.class);
    }

    @Test
    void registerVeterinary_shouldThrowInvalidVeterinaryEmailException_whenEmailIsInvalid() {
        validRequest.setEmail("esto-no-es-un-email");

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryEmailException.class);
    }

    @Test
    void registerVeterinary_shouldThrowInvalidNitException_whenNitIsNull() {
        validRequest.setNit(null);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidNitException.class);
    }

    @Test
    void registerVeterinary_shouldThrowInvalidNitException_whenCheckDigitIsWrong() {
        validRequest.setNit("123456789-0");

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidNitException.class);
    }

    @Test
    void registerVeterinary_shouldThrowInvalidNitException_whenFormatIsWrong() {
        validRequest.setNit("not-a-nit");

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidNitException.class);
    }

    @Test
    void registerVeterinary_shouldThrowVeterinaryAlreadyExistsException_whenNameAlreadyExists() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(true);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(VeterinaryAlreadyExistsException.class);
    }

    @Test
    void registerVeterinary_shouldThrowVeterinaryEmailAlreadyExistsException_whenEmailAlreadyExists() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(true);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(VeterinaryEmailAlreadyExistsException.class);
    }

    @Test
    void registerVeterinary_shouldThrowVeterinaryNitAlreadyExistsException_whenNitAlreadyExists() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByNit(VALID_NIT)).thenReturn(true);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(VeterinaryNitAlreadyExistsException.class);
    }

    @Test
    void registerVeterinary_shouldThrowTooManyOwnedVeterinariesException_whenCallerAlreadyOwnsThreeNonFinalClinics() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByNit(any())).thenReturn(false);

        List<VeterinaryEmployee> ownedRows = List.of(
                ownerRowFor(UUID.randomUUID()), ownerRowFor(UUID.randomUUID()), ownerRowFor(UUID.randomUUID()));
        when(employeeRepositoryPort.findByUserIdAndRole(callerId, EmployeeRole.OWNER)).thenReturn(ownedRows);
        for (VeterinaryEmployee row : ownedRows) {
            when(veterinaryRepositoryPort.findById(row.getVeterinaryId())).thenReturn(Optional.of(
                    Veterinary.builder().id(row.getVeterinaryId()).status(VeterinaryStatus.ACTIVE).build()));
        }

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(TooManyOwnedVeterinariesException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void registerVeterinary_shouldAllowRegistration_whenCallerOwnsThreeClinicsButOneIsRejected() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByNit(any())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        List<VeterinaryEmployee> ownedRows = List.of(
                ownerRowFor(UUID.randomUUID()), ownerRowFor(UUID.randomUUID()), ownerRowFor(UUID.randomUUID()));
        when(employeeRepositoryPort.findByUserIdAndRole(callerId, EmployeeRole.OWNER)).thenReturn(ownedRows);
        when(veterinaryRepositoryPort.findById(ownedRows.get(0).getVeterinaryId())).thenReturn(Optional.of(
                Veterinary.builder().id(ownedRows.get(0).getVeterinaryId()).status(VeterinaryStatus.REJECTED).build()));
        when(veterinaryRepositoryPort.findById(ownedRows.get(1).getVeterinaryId())).thenReturn(Optional.of(
                Veterinary.builder().id(ownedRows.get(1).getVeterinaryId()).status(VeterinaryStatus.ACTIVE).build()));
        when(veterinaryRepositoryPort.findById(ownedRows.get(2).getVeterinaryId())).thenReturn(Optional.of(
                Veterinary.builder().id(ownedRows.get(2).getVeterinaryId()).status(VeterinaryStatus.ACTIVE).build()));

        assertThatNoException().isThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        );
    }

    @Test
    void registerVeterinary_shouldCallSave_once() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByNit(any())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        registerVeterinaryService.registerVeterinary(validRequest);

        verify(veterinaryRepositoryPort, times(1)).save(any(Veterinary.class));
    }

    @Test
    void registerVeterinary_shouldSaveWithPendingDocumentsStatusAndInactiveFlag() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByNit(any())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        registerVeterinaryService.registerVeterinary(validRequest);

        verify(veterinaryRepositoryPort, times(1)).save(argThat((Veterinary v) ->
                v.getStatus() == VeterinaryStatus.PENDING_DOCUMENTS
                        && Boolean.FALSE.equals(v.getActive())
                        && v.getCreatedBy().equals(callerId)
                        && v.getNit().value().equals(VALID_NIT)
        ));
    }

    @Test
    void registerVeterinary_shouldNeverCallSave_whenValidationFails() {
        validRequest.setName(null);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryNameException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void registerVeterinary_shouldCreateCallerAsActiveOwnerEmployee_ofTheNewVeterinary() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByNit(any())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        registerVeterinaryService.registerVeterinary(validRequest);

        verify(employeeRepositoryPort, times(1)).save(argThat((VeterinaryEmployee employee) ->
                employee.getVeterinaryId().equals(savedVeterinary.getId())
                        && employee.getUserId().equals(callerId)
                        && employee.getRole() == EmployeeRole.OWNER
                        && Boolean.TRUE.equals(employee.getActive())
        ));
    }

    private static VeterinaryEmployee ownerRowFor(UUID veterinaryId) {
        return VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .role(EmployeeRole.OWNER)
                .active(true)
                .build();
    }
}
