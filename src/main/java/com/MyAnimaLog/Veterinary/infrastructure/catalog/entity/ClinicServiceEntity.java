package com.MyAnimaLog.Veterinary.infrastructure.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clinic_services")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClinicServiceEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "veterinary_id", nullable = false)
    private UUID veterinaryId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "bookable_online", nullable = false)
    private Boolean bookableOnline;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "requires_license", nullable = false)
    private Boolean requiresLicense;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_roles", nullable = false)
    private List<String> allowedRoles;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** La mantiene Hibernate (mismo patrón que {@code VeterinaryPatientEntity.updatedAt}). */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
