package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.InvalidClinicServiceException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClinicServiceValidationTest {

    @Test
    void validateName_shouldRejectBlankOrTooLong() {
        assertThatThrownBy(() -> ClinicServiceValidation.validateName(" "))
                .isInstanceOf(InvalidClinicServiceException.class);
        assertThatThrownBy(() -> ClinicServiceValidation.validateName("a".repeat(121)))
                .isInstanceOf(InvalidClinicServiceException.class);
        assertThat(ClinicServiceValidation.validateName(" Consulta ")).isEqualTo("Consulta");
    }

    @Test
    void validateCategory_shouldRejectUnknownValues() {
        assertThatThrownBy(() -> ClinicServiceValidation.validateCategory("UNKNOWN"))
                .isInstanceOf(InvalidClinicServiceException.class);
        assertThat(ClinicServiceValidation.validateCategory("SURGERY").name()).isEqualTo("SURGERY");
    }

    @Test
    void validateDurationMinutes_shouldRejectOutOfRange() {
        assertThatThrownBy(() -> ClinicServiceValidation.validateDurationMinutes(4))
                .isInstanceOf(InvalidClinicServiceException.class);
        assertThatThrownBy(() -> ClinicServiceValidation.validateDurationMinutes(601))
                .isInstanceOf(InvalidClinicServiceException.class);
        assertThat(ClinicServiceValidation.validateDurationMinutes(30)).isEqualTo(30);
    }

    @Test
    void validatePrice_shouldRejectNegativeOrMoreThanTwoDecimals() {
        assertThatThrownBy(() -> ClinicServiceValidation.validatePrice(BigDecimal.valueOf(-1)))
                .isInstanceOf(InvalidClinicServiceException.class);
        assertThatThrownBy(() -> ClinicServiceValidation.validatePrice(new BigDecimal("40.123")))
                .isInstanceOf(InvalidClinicServiceException.class);
        assertThat(ClinicServiceValidation.validatePrice(new BigDecimal("40000.00")))
                .isEqualByComparingTo("40000.00");
    }

    @Test
    void validateAllowedRoles_shouldReturnDefault_whenNull() {
        assertThat(ClinicServiceValidation.validateAllowedRoles(null))
                .containsExactly(EmployeeRole.OWNER, EmployeeRole.ADMIN, EmployeeRole.VETERINARIAN);
    }

    @Test
    void validateAllowedRoles_shouldRejectEmptyList() {
        assertThatThrownBy(() -> ClinicServiceValidation.validateAllowedRoles(List.of()))
                .isInstanceOf(InvalidClinicServiceException.class);
    }

    @Test
    void validateAllowedRoles_shouldRejectUnknownRole() {
        assertThatThrownBy(() -> ClinicServiceValidation.validateAllowedRoles(List.of("MANAGER")))
                .isInstanceOf(InvalidClinicServiceException.class);
    }

    @Test
    void validateAllowedRoles_shouldParseValidSubset() {
        assertThat(ClinicServiceValidation.validateAllowedRoles(List.of("ASSISTANT", "RECEPTIONIST")))
                .containsExactly(EmployeeRole.ASSISTANT, EmployeeRole.RECEPTIONIST);
    }
}
