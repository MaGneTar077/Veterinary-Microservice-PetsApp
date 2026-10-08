package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryProfileResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetVeterinaryProfileUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetVeterinaryProfileService implements GetVeterinaryProfileUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public GetVeterinaryProfileResponse getProfile(UUID veterinaryId) {
        authorizationService.requireMember(veterinaryId);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        return GetVeterinaryProfileResponse.builder()
                .id(veterinary.getId())
                .name(veterinary.getName())
                .city(veterinary.getCity())
                .phone(veterinary.getPhone())
                .email(veterinary.getEmail())
                .tenantId(veterinary.getTenantId())
                .active(veterinary.getActive())
                .status(veterinary.getStatus())
                .legalName(veterinary.getLegalName())
                .nit(veterinary.getNit() != null ? veterinary.getNit().value() : null)
                .address(veterinary.getAddress())
                .department(veterinary.getDepartment())
                .latitude(veterinary.getLatitude())
                .longitude(veterinary.getLongitude())
                .timezone(veterinary.getTimezone())
                .currency(veterinary.getCurrency())
                .defaultAppointmentMinutes(veterinary.getDefaultAppointmentMinutes())
                .allowOnlineBooking(veterinary.getAllowOnlineBooking())
                .bookingRequiresConfirmation(veterinary.getBookingRequiresConfirmation())
                .cancellationMinHours(veterinary.getCancellationMinHours())
                .directoryVisible(veterinary.getDirectoryVisible())
                .inviteCode(veterinary.getInviteCode())
                .inviteLink(veterinary.getInviteLink())
                .createdBy(veterinary.getCreatedBy())
                .createdAt(veterinary.getCreatedAt())
                .updatedAt(veterinary.getUpdatedAt())
                .approvedAt(veterinary.getApprovedAt())
                .build();
    }
}
