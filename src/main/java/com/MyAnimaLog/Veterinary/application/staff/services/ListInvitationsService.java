package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.InvitationSummary;
import com.MyAnimaLog.Veterinary.application.staff.dto.ListInvitationsResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ListInvitationsUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListInvitationsService implements ListInvitationsUseCase {

    private final EmployeeInvitationRepositoryPort invitationRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public ListInvitationsResponse list(UUID veterinaryId, InvitationStatus status) {
        authorizationService.require(veterinaryId, Permission.STAFF_MANAGE);

        LocalDateTime now = LocalDateTime.now();
        List<InvitationSummary> invitations = invitationRepositoryPort
                .findByVeterinaryIdAndStatus(veterinaryId, status).stream()
                .map(invitation -> InvitationSummaries.of(invitation, now))
                .toList();

        return ListInvitationsResponse.builder()
                .invitations(invitations)
                .build();
    }
}
