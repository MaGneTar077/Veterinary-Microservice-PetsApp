package com.MyAnimaLog.Veterinary.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_veterinary_link")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserVeterinaryLinkEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "veterinary_id", nullable = false)
    private UUID veterinaryId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
}