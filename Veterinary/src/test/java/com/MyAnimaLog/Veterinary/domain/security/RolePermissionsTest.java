package com.MyAnimaLog.Veterinary.domain.security;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static com.MyAnimaLog.Veterinary.domain.security.Permission.APPOINTMENT_MANAGE;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.CLINICAL_READ;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.CLINICAL_READ_BASIC;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.CLINICAL_WRITE;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.CLINIC_CONFIGURE;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.DOCUMENT_UPLOAD;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.NURSING_WRITE;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.PATIENT_REGISTER;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.REPORTS_VIEW;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.STAFF_MANAGE;
import static com.MyAnimaLog.Veterinary.domain.security.Permission.SUBSCRIPTION_MANAGE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * One case per combination of role x licensed x clinic-status. For each case the whole
 * resulting {@link Permission} set is compared, which implicitly covers every permission
 * (present and absent) for that combination — see CLAUDE.md for the matrix this encodes.
 */
class RolePermissionsTest {

    private static final Set<Permission> ADMIN_BASE = EnumSet.of(
            CLINIC_CONFIGURE, STAFF_MANAGE, APPOINTMENT_MANAGE, PATIENT_REGISTER,
            CLINICAL_READ, CLINICAL_READ_BASIC, DOCUMENT_UPLOAD, REPORTS_VIEW);

    private static final Set<Permission> VET_ASSISTANT_BASE = EnumSet.of(
            APPOINTMENT_MANAGE, PATIENT_REGISTER, CLINICAL_READ, CLINICAL_READ_BASIC,
            NURSING_WRITE, DOCUMENT_UPLOAD);

    private static final Set<Permission> OWNER_BASE = plus(ADMIN_BASE, SUBSCRIPTION_MANAGE);

    private static final Set<Permission> RECEPTIONIST_BASE = EnumSet.of(
            APPOINTMENT_MANAGE, PATIENT_REGISTER, CLINICAL_READ_BASIC);

    private static final Set<Permission> ADMIN_PRE_APPROVAL = EnumSet.of(CLINIC_CONFIGURE, STAFF_MANAGE);
    private static final Set<Permission> OWNER_PRE_APPROVAL = EnumSet.of(CLINIC_CONFIGURE, STAFF_MANAGE, SUBSCRIPTION_MANAGE);
    private static final Set<Permission> NONE = EnumSet.noneOf(Permission.class);

    static Stream<Arguments> cases() {
        return Stream.of(
                // --- status ACTIVE ---
                Arguments.of(EmployeeRole.OWNER, false, VeterinaryStatus.ACTIVE, OWNER_BASE),
                Arguments.of(EmployeeRole.OWNER, true, VeterinaryStatus.ACTIVE, plus(OWNER_BASE, CLINICAL_WRITE, NURSING_WRITE)),
                Arguments.of(EmployeeRole.ADMIN, false, VeterinaryStatus.ACTIVE, ADMIN_BASE),
                Arguments.of(EmployeeRole.ADMIN, true, VeterinaryStatus.ACTIVE, plus(ADMIN_BASE, CLINICAL_WRITE, NURSING_WRITE)),
                Arguments.of(EmployeeRole.VETERINARIAN, false, VeterinaryStatus.ACTIVE, VET_ASSISTANT_BASE),
                Arguments.of(EmployeeRole.VETERINARIAN, true, VeterinaryStatus.ACTIVE, plus(VET_ASSISTANT_BASE, CLINICAL_WRITE)),
                Arguments.of(EmployeeRole.ASSISTANT, false, VeterinaryStatus.ACTIVE, VET_ASSISTANT_BASE),
                Arguments.of(EmployeeRole.ASSISTANT, true, VeterinaryStatus.ACTIVE, VET_ASSISTANT_BASE),
                Arguments.of(EmployeeRole.RECEPTIONIST, false, VeterinaryStatus.ACTIVE, RECEPTIONIST_BASE),
                Arguments.of(EmployeeRole.RECEPTIONIST, true, VeterinaryStatus.ACTIVE, RECEPTIONIST_BASE),

                // --- status SUSPENDED (status != ACTIVE) ---
                Arguments.of(EmployeeRole.OWNER, false, VeterinaryStatus.SUSPENDED, OWNER_PRE_APPROVAL),
                Arguments.of(EmployeeRole.OWNER, true, VeterinaryStatus.SUSPENDED, OWNER_PRE_APPROVAL),
                Arguments.of(EmployeeRole.ADMIN, false, VeterinaryStatus.SUSPENDED, ADMIN_PRE_APPROVAL),
                Arguments.of(EmployeeRole.ADMIN, true, VeterinaryStatus.SUSPENDED, ADMIN_PRE_APPROVAL),
                Arguments.of(EmployeeRole.VETERINARIAN, false, VeterinaryStatus.SUSPENDED, NONE),
                Arguments.of(EmployeeRole.VETERINARIAN, true, VeterinaryStatus.SUSPENDED, NONE),
                Arguments.of(EmployeeRole.ASSISTANT, false, VeterinaryStatus.SUSPENDED, NONE),
                Arguments.of(EmployeeRole.ASSISTANT, true, VeterinaryStatus.SUSPENDED, NONE),
                Arguments.of(EmployeeRole.RECEPTIONIST, false, VeterinaryStatus.SUSPENDED, NONE),

                // --- status UNDER_REVIEW: otro valor != ACTIVE, para probar que la regla
                // es "distinto de ACTIVE" y no un valor puntual como SUSPENDED ---
                Arguments.of(EmployeeRole.OWNER, false, VeterinaryStatus.UNDER_REVIEW, OWNER_PRE_APPROVAL),
                Arguments.of(EmployeeRole.ADMIN, false, VeterinaryStatus.UNDER_REVIEW, ADMIN_PRE_APPROVAL),
                Arguments.of(EmployeeRole.ADMIN, true, VeterinaryStatus.UNDER_REVIEW, ADMIN_PRE_APPROVAL),
                Arguments.of(EmployeeRole.VETERINARIAN, false, VeterinaryStatus.UNDER_REVIEW, NONE),
                Arguments.of(EmployeeRole.VETERINARIAN, true, VeterinaryStatus.UNDER_REVIEW, NONE),
                Arguments.of(EmployeeRole.ASSISTANT, false, VeterinaryStatus.UNDER_REVIEW, NONE),
                Arguments.of(EmployeeRole.ASSISTANT, true, VeterinaryStatus.UNDER_REVIEW, NONE),
                Arguments.of(EmployeeRole.RECEPTIONIST, false, VeterinaryStatus.UNDER_REVIEW, NONE)
        );
    }

    @ParameterizedTest(name = "{0}, licensed={1}, status={2}")
    @MethodSource("cases")
    void resolve_returnsExpectedPermissionSet(EmployeeRole role, boolean licensed, VeterinaryStatus status,
                                                Set<Permission> expected) {
        assertThat(RolePermissions.resolve(role, licensed, status)).isEqualTo(expected);
    }

    private static Set<Permission> plus(Set<Permission> base, Permission... extra) {
        Set<Permission> result = EnumSet.copyOf(base);
        result.addAll(Arrays.asList(extra));
        return result;
    }
}
