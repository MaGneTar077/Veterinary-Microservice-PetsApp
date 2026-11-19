package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.InvalidClinicServiceException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reglas de validación compartidas por {@code CreateClinicService} y {@code UpdateClinicService}
 * (todo lo que valida el body, no la autorización ni la unicidad del nombre — esa necesita una
 * consulta y se queda en cada servicio). Utilidad estática, no un puerto ni un servicio.
 */
public final class ClinicServiceValidation {

    public static final List<EmployeeRole> DEFAULT_ALLOWED_ROLES =
            List.of(EmployeeRole.OWNER, EmployeeRole.ADMIN, EmployeeRole.VETERINARIAN);

    private ClinicServiceValidation() {
    }

    public static String validateName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new InvalidClinicServiceException("Service name must be between 1 and 120 characters");
        }
        return name.trim();
    }

    public static ServiceCategory validateCategory(String category) {
        if (category == null) {
            throw new InvalidClinicServiceException("Service category is required");
        }
        try {
            return ServiceCategory.valueOf(category);
        } catch (IllegalArgumentException ex) {
            throw new InvalidClinicServiceException("Invalid service category: " + category);
        }
    }

    public static Integer validateDurationMinutes(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < 5 || durationMinutes > 600) {
            throw new InvalidClinicServiceException("Duration must be between 5 and 600 minutes");
        }
        return durationMinutes;
    }

    public static BigDecimal validatePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new InvalidClinicServiceException("Price must be zero or greater");
        }
        if (price.scale() > 2) {
            throw new InvalidClinicServiceException("Price must have at most two decimals");
        }
        return price;
    }

    /** {@code null} usa el default {@link #DEFAULT_ALLOWED_ROLES}; una lista vacía es inválida. */
    public static List<EmployeeRole> validateAllowedRoles(List<String> allowedRoles) {
        if (allowedRoles == null) {
            return DEFAULT_ALLOWED_ROLES;
        }
        if (allowedRoles.isEmpty()) {
            throw new InvalidClinicServiceException("allowedRoles cannot be empty");
        }
        return allowedRoles.stream()
                .map(ClinicServiceValidation::parseRole)
                .distinct()
                .toList();
    }

    private static EmployeeRole parseRole(String role) {
        try {
            return EmployeeRole.valueOf(role);
        } catch (IllegalArgumentException ex) {
            throw new InvalidClinicServiceException("Invalid employee role in allowedRoles: " + role);
        }
    }
}
