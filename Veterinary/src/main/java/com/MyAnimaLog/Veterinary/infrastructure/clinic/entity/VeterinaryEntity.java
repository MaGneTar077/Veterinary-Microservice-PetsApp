package com.MyAnimaLog.Veterinary.infrastructure.clinic.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "nit")
    private String nit;

    @Column(name = "address")
    private String address;

    @Column(name = "department")
    private String department;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "default_appointment_minutes", nullable = false)
    private Integer defaultAppointmentMinutes;

    @Column(name = "allow_online_booking", nullable = false)
    private Boolean allowOnlineBooking;

    @Column(name = "booking_requires_confirmation", nullable = false)
    private Boolean bookingRequiresConfirmation;

    @Column(name = "cancellation_min_hours", nullable = false)
    private Integer cancellationMinHours;

    @Column(name = "directory_visible", nullable = false)
    private Boolean directoryVisible;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
