package com.MyAnimaLog.Veterinary.infrastructure.clinic.repositories;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.infrastructure.clinic.entity.VeterinaryEntity;
import com.MyAnimaLog.Veterinary.infrastructure.subscription.entity.VeterinarySubscriptionEntity;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the WHERE clause for {@code GET /api/veterinary/admin/veterinaries} one condition at a
 * time, adding a predicate only for the filters that are actually present. Replaces a JPQL
 * {@code (:param IS NULL OR ...)} query that used to 500 whenever a filter was absent: Hibernate 6
 * can't always infer a JDBC type for a parameter whose only usage is "compare to NULL", and
 * PostgreSQL then rejects it (sends it as {@code bytea}, "function lower(bytea) does not exist" —
 * confirmed by reproducing against the real database). Omitting the predicate entirely sidesteps
 * the problem instead of working around Hibernate's type inference.
 */
public final class VeterinaryAdminSpecifications {

    private VeterinaryAdminSpecifications() {
    }

    public static Specification<VeterinaryEntity> build(VeterinaryStatus status, String search, String plan,
                                                          String city, String department) {
        List<Specification<VeterinaryEntity>> specs = new ArrayList<>();

        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status.name()));
        }

        String trimmedSearch = trimToNull(search);
        if (trimmedSearch != null) {
            String pattern = likePattern(trimmedSearch);
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("legalName")), pattern),
                    cb.like(cb.lower(root.get("nit")), pattern)));
        }

        String trimmedCity = trimToNull(city);
        if (trimmedCity != null) {
            String lowerCity = trimmedCity.toLowerCase();
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.get("city")), lowerCity));
        }

        String trimmedDepartment = trimToNull(department);
        if (trimmedDepartment != null) {
            String lowerDepartment = trimmedDepartment.toLowerCase();
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.get("department")), lowerDepartment));
        }

        String trimmedPlan = trimToNull(plan);
        if (trimmedPlan != null) {
            specs.add((root, query, cb) -> {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<VeterinarySubscriptionEntity> subscription = subquery.from(VeterinarySubscriptionEntity.class);
                subquery.select(subscription.get("veterinaryId"))
                        .where(cb.equal(subscription.get("veterinaryId"), root.get("id")),
                                cb.isTrue(subscription.get("active")),
                                cb.equal(subscription.get("plan"), trimmedPlan));
                return cb.exists(subquery);
            });
        }

        return Specification.allOf(specs);
    }

    private static String likePattern(String value) {
        return "%" + value.toLowerCase() + "%";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
