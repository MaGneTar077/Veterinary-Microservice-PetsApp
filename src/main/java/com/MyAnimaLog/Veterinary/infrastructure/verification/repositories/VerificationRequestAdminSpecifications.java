package com.MyAnimaLog.Veterinary.infrastructure.verification.repositories;

import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.infrastructure.verification.entity.VerificationRequestEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WHERE clause for {@code GET /api/veterinary/admin/verifications} one condition at a
 * time, adding a predicate only for the filters that are actually present — same reasoning as
 * {@code VeterinaryAdminSpecifications}: a JPQL {@code (:param IS NULL OR ...)} query 500s
 * whenever {@code subjectType} is absent, because Hibernate 6 can't always infer a JDBC type for
 * a parameter only ever compared to NULL, and PostgreSQL rejects the resulting untyped bind.
 * {@code status} normally always has a value (the service defaults it to {@code UNDER_REVIEW}),
 * but this builder treats it the same way defensively.
 */
public final class VerificationRequestAdminSpecifications {

    private VerificationRequestAdminSpecifications() {
    }

    public static Specification<VerificationRequestEntity> build(VerificationRequestStatus status,
                                                                   SubjectType subjectType) {
        List<Specification<VerificationRequestEntity>> specs = new ArrayList<>();

        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (subjectType != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("subjectType"), subjectType));
        }

        return Specification.allOf(specs);
    }
}
