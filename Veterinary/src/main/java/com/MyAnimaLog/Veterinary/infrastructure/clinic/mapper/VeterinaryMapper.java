package com.MyAnimaLog.Veterinary.infrastructure.clinic.mapper;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.infrastructure.clinic.entity.VeterinaryEntity;
import org.springframework.stereotype.Component;

@Component
public class VeterinaryMapper {
    public VeterinaryEntity toEntity(Veterinary domain) {
        return VeterinaryEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .city(domain.getCity())
                .phone(domain.getPhone())
                .email(domain.getEmail())
                .inviteCode(domain.getInviteCode())
                .inviteLink(domain.getInviteLink())
                .tenantId(domain.getTenantId())
                .active(domain.getActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .legalName(domain.getLegalName())
                .nit(domain.getNit() != null ? domain.getNit().value() : null)
                .address(domain.getAddress())
                .department(domain.getDepartment())
                .latitude(domain.getLatitude())
                .longitude(domain.getLongitude())
                .timezone(domain.getTimezone())
                .currency(domain.getCurrency())
                .defaultAppointmentMinutes(domain.getDefaultAppointmentMinutes())
                .allowOnlineBooking(domain.getAllowOnlineBooking())
                .bookingRequiresConfirmation(domain.getBookingRequiresConfirmation())
                .cancellationMinHours(domain.getCancellationMinHours())
                .directoryVisible(domain.getDirectoryVisible())
                .createdBy(domain.getCreatedBy())
                .approvedAt(domain.getApprovedAt())
                .build();
    }

    public Veterinary toDomain(VeterinaryEntity entity) {
        return Veterinary.builder()
                .id(entity.getId())
                .name(entity.getName())
                .city(entity.getCity())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .inviteCode(entity.getInviteCode())
                .inviteLink(entity.getInviteLink())
                .tenantId(entity.getTenantId())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .status(entity.getStatus() != null ? VeterinaryStatus.valueOf(entity.getStatus()) : null)
                .legalName(entity.getLegalName())
                .nit(entity.getNit() != null ? Nit.fromPersisted(entity.getNit()) : null)
                .address(entity.getAddress())
                .department(entity.getDepartment())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .timezone(entity.getTimezone())
                .currency(entity.getCurrency())
                .defaultAppointmentMinutes(entity.getDefaultAppointmentMinutes())
                .allowOnlineBooking(entity.getAllowOnlineBooking())
                .bookingRequiresConfirmation(entity.getBookingRequiresConfirmation())
                .cancellationMinHours(entity.getCancellationMinHours())
                .directoryVisible(entity.getDirectoryVisible())
                .createdBy(entity.getCreatedBy())
                .approvedAt(entity.getApprovedAt())
                .build();
    }
}
