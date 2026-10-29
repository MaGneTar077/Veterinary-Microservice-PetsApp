package com.MyAnimaLog.Veterinary.application.shared.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.ClinicMembership;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicContextRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicSuspendedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotClinicMemberException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lo que este test protege (Fase 3, PASO 0b): <strong>ninguna decisión de autorización se toma con
 * los claims del token</strong>. El rol y el estado de la clínica salen de
 * {@code findClinicMembership}, así que a un empleado al que retiran o al que le bajan el rol se le
 * cierra la puerta en la siguiente petición, sin esperar a que expire su token de clínica (hasta
 * 60 min). Los casos clave son los que ponen un claim "bueno" contra una fila "mala".
 */
@ExtendWith(MockitoExtension.class)
class VeterinaryAuthorizationServiceTest {

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @InjectMocks
    private VeterinaryAuthorizationService authorizationService;

    private UUID veterinaryId;
    private UUID userId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        userId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    private AuthenticatedUser normalToken() {
        return new AuthenticatedUser(userId, "user@example.com", true, false, Optional.empty());
    }

    private AuthenticatedUser clinicToken(EmployeeRole claimRole, VeterinaryStatus claimStatus) {
        return clinicToken(veterinaryId, claimRole, claimStatus);
    }

    private AuthenticatedUser clinicToken(UUID claimVetId, EmployeeRole claimRole, VeterinaryStatus claimStatus) {
        return new AuthenticatedUser(userId, "user@example.com", true, false,
                Optional.of(new ClinicContext(claimVetId, employeeId, claimRole, false, claimStatus)));
    }

    private void membership(EmployeeRole role, boolean active, VeterinaryStatus clinicStatus) {
        lenient().when(employeeRepositoryPort.findClinicMembership(veterinaryId, userId))
                .thenReturn(Optional.of(new ClinicMembership(employeeId, role, active, clinicStatus)));
    }

    private void noMembership() {
        lenient().when(employeeRepositoryPort.findClinicMembership(veterinaryId, userId))
                .thenReturn(Optional.empty());
    }

    // ---------- El claim no decide: la base decide ----------

    @Test
    void require_shouldUseTheRoleFromTheDatabase_notTheOneInTheToken() {
        // El token dice ADMIN (tiene STAFF_MANAGE); la base dice RECEPTIONIST (no lo tiene).
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.RECEPTIONIST, true, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() -> authorizationService.require(veterinaryId, Permission.STAFF_MANAGE))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void require_shouldGrantThePermission_whenTheDatabaseRoleHasIt_evenIfTheClaimIsLower() {
        // Caso inverso: al empleado lo promovieron después de emitir el token.
        when(authenticatedUserPort.current())
                .thenReturn(clinicToken(EmployeeRole.RECEPTIONIST, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.ADMIN, true, VeterinaryStatus.ACTIVE);

        ClinicContext context = authorizationService.require(veterinaryId, Permission.STAFF_MANAGE);

        assertThat(context.role()).isEqualTo(EmployeeRole.ADMIN);
    }

    @Test
    void requireMember_shouldThrowNotClinicMember_whenTheEmployeeWasRemovedAfterTheTokenWasIssued() {
        when(authenticatedUserPort.current())
                .thenReturn(clinicToken(EmployeeRole.VETERINARIAN, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.VETERINARIAN, false, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() -> authorizationService.requireMember(veterinaryId))
                .isInstanceOf(NotClinicMemberException.class);
    }

    @Test
    void requireMember_shouldThrowNotClinicMember_whenThereIsNoEmployeeRowAtAll() {
        when(authenticatedUserPort.current())
                .thenReturn(clinicToken(EmployeeRole.VETERINARIAN, VeterinaryStatus.ACTIVE));
        noMembership();

        assertThatThrownBy(() -> authorizationService.requireMember(veterinaryId))
                .isInstanceOf(NotClinicMemberException.class);
    }

    @Test
    void require_shouldThrowNotClinicMember_whenTheEmployeeWasRemoved_evenIfTheClaimSaysOwner() {
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.OWNER, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.OWNER, false, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() -> authorizationService.require(veterinaryId, Permission.STAFF_MANAGE))
                .isInstanceOf(NotClinicMemberException.class);
    }

    @Test
    void requireActiveMember_shouldUseTheClinicStatusFromTheDatabase_notTheClaim() {
        // El claim dice ACTIVE porque la suspensión ocurrió después de emitir el token.
        when(authenticatedUserPort.current())
                .thenReturn(clinicToken(EmployeeRole.VETERINARIAN, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.VETERINARIAN, true, VeterinaryStatus.SUSPENDED);

        assertThatThrownBy(() -> authorizationService.requireActiveMember(veterinaryId))
                .isInstanceOf(ClinicSuspendedException.class);
    }

    @Test
    void require_shouldApplyThePreApprovalPermissionTrimming_withTheStatusFromTheDatabase() {
        // Clínica no aprobada: RolePermissions deja solo CLINIC_CONFIGURE/STAFF_MANAGE/SUBSCRIPTION_MANAGE.
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.OWNER, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.OWNER, true, VeterinaryStatus.PENDING_DOCUMENTS);

        assertThatNoException().isThrownBy(() ->
                authorizationService.require(veterinaryId, Permission.STAFF_MANAGE));
        assertThatThrownBy(() -> authorizationService.require(veterinaryId, Permission.APPOINTMENT_MANAGE))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void require_shouldNotGrantClinicalWrite_whenTheClaimSaysLicensedButTheDatabaseRoleCannotHaveIt() {
        // licensed sigue saliendo del claim (deuda técnica conocida), pero solo puede sumar
        // CLINICAL_WRITE sobre un rol que la matriz permite: nunca sobre RECEPTIONIST.
        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(userId, "user@example.com",
                true, false,
                Optional.of(new ClinicContext(veterinaryId, employeeId, EmployeeRole.VETERINARIAN,
                        true, VeterinaryStatus.ACTIVE))));
        membership(EmployeeRole.RECEPTIONIST, true, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() -> authorizationService.require(veterinaryId, Permission.CLINICAL_WRITE))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void require_shouldGrantClinicalWrite_whenLicensedAndTheDatabaseRoleAllowsIt() {
        when(authenticatedUserPort.current()).thenReturn(new AuthenticatedUser(userId, "user@example.com",
                true, false,
                Optional.of(new ClinicContext(veterinaryId, employeeId, EmployeeRole.RECEPTIONIST,
                        true, VeterinaryStatus.ACTIVE))));
        membership(EmployeeRole.VETERINARIAN, true, VeterinaryStatus.ACTIVE);

        assertThatNoException().isThrownBy(() ->
                authorizationService.require(veterinaryId, Permission.CLINICAL_WRITE));
    }

    @Test
    void requireActiveMember_shouldReturnTheEmployeeIdFromTheDatabase() {
        UUID realEmployeeId = UUID.randomUUID();
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));
        when(employeeRepositoryPort.findClinicMembership(veterinaryId, userId)).thenReturn(
                Optional.of(new ClinicMembership(realEmployeeId, EmployeeRole.ADMIN, true, VeterinaryStatus.ACTIVE)));

        assertThat(authorizationService.requireActiveMember(veterinaryId).employeeId())
                .isEqualTo(realEmployeeId);
    }

    @Test
    void requireActiveMember_shouldResolveEverythingInASingleQuery() {
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.ADMIN, true, VeterinaryStatus.ACTIVE);

        authorizationService.requireActiveMember(veterinaryId);

        verify(employeeRepositoryPort).findClinicMembership(veterinaryId, userId);
        verify(employeeRepositoryPort, never()).findByVeterinaryIdAndUserId(veterinaryId, userId);
        verify(employeeRepositoryPort, never()).findById(employeeId);
    }

    @Test
    void requireMember_shouldIgnoreSuspension() {
        // GetVeterinaryProfile tiene que poder mostrar por qué la clínica está suspendida.
        when(authenticatedUserPort.current())
                .thenReturn(clinicToken(EmployeeRole.VETERINARIAN, VeterinaryStatus.SUSPENDED));
        membership(EmployeeRole.VETERINARIAN, true, VeterinaryStatus.SUSPENDED);

        assertThat(authorizationService.requireMember(veterinaryId).status())
                .isEqualTo(VeterinaryStatus.SUSPENDED);
    }

    // ---------- Contexto y tenant ----------

    @Test
    void requireMember_shouldThrowClinicContextRequired_whenTheTokenIsNotAClinicToken() {
        when(authenticatedUserPort.current()).thenReturn(normalToken());

        assertThatThrownBy(() -> authorizationService.requireMember(veterinaryId))
                .isInstanceOf(ClinicContextRequiredException.class);

        verify(employeeRepositoryPort, never()).findClinicMembership(veterinaryId, userId);
    }

    @Test
    void requireMember_shouldThrowTenantMismatch_beforeTouchingTheDatabase() {
        when(authenticatedUserPort.current())
                .thenReturn(clinicToken(UUID.randomUUID(), EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));

        assertThatThrownBy(() -> authorizationService.requireMember(veterinaryId))
                .isInstanceOf(TenantMismatchException.class);

        verify(employeeRepositoryPort, never()).findClinicMembership(veterinaryId, userId);
    }

    // ---------- requireVeterinaryOwner ----------

    @Test
    void requireVeterinaryOwner_shouldPass_whenTheDatabaseSaysActiveOwner_withANormalToken() {
        when(authenticatedUserPort.current()).thenReturn(normalToken());
        membership(EmployeeRole.OWNER, true, VeterinaryStatus.ACTIVE);

        assertThatNoException().isThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId));
    }

    @Test
    void requireVeterinaryOwner_shouldPass_evenWhenTheClinicIsRejectedOrSuspended() {
        // El dueño tiene que poder arreglar su verificación en cualquier estado.
        when(authenticatedUserPort.current()).thenReturn(normalToken());
        membership(EmployeeRole.OWNER, true, VeterinaryStatus.REJECTED);

        assertThatNoException().isThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId));
    }

    @Test
    void requireVeterinaryOwner_shouldThrowNotVeterinaryOwner_whenTheDatabaseSaysAdmin_evenIfTheClaimSaysOwner() {
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.OWNER, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.ADMIN, true, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    @Test
    void requireVeterinaryOwner_shouldThrowNotVeterinaryOwner_whenTheOwnerRowIsInactive() {
        when(authenticatedUserPort.current()).thenReturn(normalToken());
        membership(EmployeeRole.OWNER, false, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    @Test
    void requireVeterinaryOwner_shouldThrowNotVeterinaryOwner_whenThereIsNoEmployeeRow() {
        when(authenticatedUserPort.current()).thenReturn(normalToken());
        noMembership();

        assertThatThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    @Test
    void requireVeterinaryOwner_shouldThrowTenantMismatch_whenTheClinicTokenIsForAnotherVeterinary() {
        when(authenticatedUserPort.current())
                .thenReturn(clinicToken(UUID.randomUUID(), EmployeeRole.OWNER, VeterinaryStatus.ACTIVE));

        assertThatThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId))
                .isInstanceOf(TenantMismatchException.class);

        verify(employeeRepositoryPort, never()).findClinicMembership(veterinaryId, userId);
    }

    // ---------- requireOwnerOrPermission ----------

    @Test
    void requireOwnerOrPermission_shouldPass_whenTheDatabaseRoleHasThePermission() {
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.ADMIN, true, VeterinaryStatus.ACTIVE);

        assertThatNoException().isThrownBy(() ->
                authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE));
    }

    @Test
    void requireOwnerOrPermission_shouldFallBackToTheOwnerCheck_whenTheRoleLacksThePermission() {
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.OWNER, VeterinaryStatus.ACTIVE));
        // OWNER sobre una clínica no aprobada: conserva CLINIC_CONFIGURE, pero la vía que importa es
        // que se lo reconoce como dueño igual.
        membership(EmployeeRole.OWNER, true, VeterinaryStatus.PENDING_DOCUMENTS);

        assertThatNoException().isThrownBy(() ->
                authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE));
    }

    @Test
    void requireOwnerOrPermission_shouldPass_whenCallerIsOwnerWithANormalToken() {
        when(authenticatedUserPort.current()).thenReturn(normalToken());
        membership(EmployeeRole.OWNER, true, VeterinaryStatus.ACTIVE);

        assertThatNoException().isThrownBy(() ->
                authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE));
    }

    @Test
    void requireOwnerOrPermission_shouldThrowClinicSuspended_whenTheClinicIsSuspendedInTheDatabase() {
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.ADMIN, true, VeterinaryStatus.SUSPENDED);

        assertThatThrownBy(() ->
                authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE))
                .isInstanceOf(ClinicSuspendedException.class);
    }

    @Test
    void requireOwnerOrPermission_shouldThrowClinicSuspended_whenCallerIsTheOwnerButTheClinicIsSuspended() {
        when(authenticatedUserPort.current()).thenReturn(normalToken());
        membership(EmployeeRole.OWNER, true, VeterinaryStatus.SUSPENDED);

        assertThatThrownBy(() ->
                authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE))
                .isInstanceOf(ClinicSuspendedException.class);
    }

    @Test
    void requireOwnerOrPermission_shouldThrowNotVeterinaryOwner_whenCallerWasRemovedFromTheClinic() {
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.ADMIN, false, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() ->
                authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    // ---------- requireSelfOrPermission ----------

    @Test
    void requireSelfOrPermission_shouldPass_whenCallerActsOnOwnRecord_withoutTouchingTheDatabase() {
        when(authenticatedUserPort.current()).thenReturn(normalToken());

        assertThatNoException().isThrownBy(() ->
                authorizationService.requireSelfOrPermission(userId, veterinaryId, Permission.CLINIC_CONFIGURE));

        verify(employeeRepositoryPort, never()).findClinicMembership(veterinaryId, userId);
    }

    @Test
    void requireSelfOrPermission_shouldUseTheDatabaseRole_whenActingOnBehalfOfAnotherUser() {
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.RECEPTIONIST, true, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() -> authorizationService.requireSelfOrPermission(
                UUID.randomUUID(), veterinaryId, Permission.CLINIC_CONFIGURE))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void requireSelfOrPermission_shouldThrowClinicSuspended_whenActingOnAnotherUserAndTheClinicIsSuspended() {
        when(authenticatedUserPort.current()).thenReturn(clinicToken(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));
        membership(EmployeeRole.ADMIN, true, VeterinaryStatus.SUSPENDED);

        assertThatThrownBy(() -> authorizationService.requireSelfOrPermission(
                UUID.randomUUID(), veterinaryId, Permission.CLINIC_CONFIGURE))
                .isInstanceOf(ClinicSuspendedException.class);
    }

    @Test
    void requireSelfOrPermission_shouldThrowInsufficientPermission_whenActingOnAnotherUserWithoutClinicContext() {
        when(authenticatedUserPort.current()).thenReturn(normalToken());

        assertThatThrownBy(() -> authorizationService.requireSelfOrPermission(
                UUID.randomUUID(), veterinaryId, Permission.CLINIC_CONFIGURE))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void requireSelfOrPermission_shouldThrowInsufficientPermission_whenTheClinicTokenIsForAnotherVeterinary() {
        when(authenticatedUserPort.current())
                .thenReturn(clinicToken(UUID.randomUUID(), EmployeeRole.OWNER, VeterinaryStatus.ACTIVE));

        assertThatThrownBy(() -> authorizationService.requireSelfOrPermission(
                UUID.randomUUID(), veterinaryId, Permission.CLINIC_CONFIGURE))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}
