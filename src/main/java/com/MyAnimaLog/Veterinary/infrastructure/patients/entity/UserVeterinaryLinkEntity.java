package com.MyAnimaLog.Veterinary.infrastructure.patients.entity;

import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LinkStatus status;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @Column(name = "unlinked_at")
    private LocalDateTime unlinkedAt;

    @Column(name = "owner_name", length = 200)
    private String ownerName;

    @Column(name = "owner_email", length = 200)
    private String ownerEmail;

    @Column(name = "owner_phone", length = 30)
    private String ownerPhone;
}
