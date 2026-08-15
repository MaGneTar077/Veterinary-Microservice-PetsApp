package com.MyAnimaLog.Veterinary.infrastructure.entity;

import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "veterinary_employee")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VeterinaryEmployeeEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "veterinary_id", nullable = false)
    private UUID veterinaryId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private EmployeeRole role;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
