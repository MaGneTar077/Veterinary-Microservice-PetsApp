package com.MyAnimaLog.Veterinary.infrastructure.professional.repositories;

import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.infrastructure.professional.entity.ProfessionalProfileEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WHERE clause for {@code GET /api/veterinary/admin/professionals} one condition at a
 * time, adding a predicate only for the filters that are actually present — same reasoning as
 * {@code VeterinaryAdminSpecifications}: a JPQL {@code (:param IS NULL OR ...)} query 500s
 * whenever a filter is absent, because Hibernate 6 can't always infer a JDBC type for a parameter
 * only ever compared to NULL, and PostgreSQL rejects the resulting untyped bind.
 */
public final class ProfessionalProfileAdminSpecifications {

    private ProfessionalProfileAdminSpecifications() {
    }

    public static Specification<ProfessionalProfileEntity> build(Boolean verified, String search) {
        List<Specification<ProfessionalProfileEntity>> specs = new ArrayList<>();

        if (verified != null) {
            if (verified) {
                specs.add((root, query, cb) -> cb.equal(root.get("licenseStatus"), LicenseStatus.VERIFIED));
            } else {
                specs.add((root, query, cb) -> cb.notEqual(root.get("licenseStatus"), LicenseStatus.VERIFIED));
            }
        }

        String trimmedSearch = trimToNull(search);
        if (trimmedSearch != null) {
            String pattern = "%" + trimmedSearch.toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("licenseNumber")), pattern)));
        }

        return Specification.allOf(specs);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
