package com.MyAnimaLog.Veterinary.infrastructure.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "veterinary")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VeterinaryEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "city")
    private String city;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "invite_code")
    private String inviteCode;

    @Column(name = "invite_link")
    private String inviteLink;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
