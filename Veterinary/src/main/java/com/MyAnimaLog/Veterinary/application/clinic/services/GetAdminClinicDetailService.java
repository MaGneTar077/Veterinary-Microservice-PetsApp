package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicDetailResponse;
import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminEmployeeInfo;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetAdminClinicDetailUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationHistoryItem;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAdminClinicDetailService implements GetAdminClinicDetailUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetAdminClinicDetailService.class);

    private final VeterinaryAuthorizationService authorizationService;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final UserServicePort userServicePort;

    @Override
    public AdminClinicDetailResponse getDetail(UUID veterinaryId) {
        authorizationService.requirePlatformAdmin();

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        List<VeterinaryEmployee> employees = employeeRepositoryPort.findByVeterinaryId(veterinaryId);

        VeterinaryEmployee owner = employees.stream()
                .filter(e -> e.getRole() == EmployeeRole.OWNER && Boolean.TRUE.equals(e.getActive()))
                .findFirst()
                .orElse(null);

        String plan = subscriptionRepositoryPort.findActiveByVeterinaryId(veterinaryId)
                .map(sub -> sub.getPlan())
                .orElse(null);

        List<AdminEmployeeInfo> employeeInfos = employees.stream()
                .map(e -> AdminEmployeeInfo.builder()
                        .employeeId(e.getId())
                        .userId(e.getUserId())
                        .role(e.getRole())
                        .active(e.getActive())
                        .build())
                .toList();

        List<AdminVerificationHistoryItem> history = verificationRequestRepositoryPort
                .findBySubjectTypeAndSubjectId(SubjectType.CLINIC, veterinaryId).stream()
                .map(r -> AdminVerificationHistoryItem.builder()
                        .attemptNumber(r.getAttemptNumber())
                        .status(r.getStatus())
                        .rejectionReasonCode(r.getRejectionReasonCode())
                        .reviewerNotes(r.getReviewerNotes())
                        .decidedAt(r.getDecidedAt())
                        .build())
                .toList();

        return AdminClinicDetailResponse.builder()
                .id(veterinary.getId())
                .name(veterinary.getName())
                .legalName(veterinary.getLegalName())
                .nit(veterinary.getNit() != null ? veterinary.getNit().value() : null)
                .phone(veterinary.getPhone())
                .email(veterinary.getEmail())
                .address(veterinary.getAddress())
                .city(veterinary.getCity())
                .department(veterinary.getDepartment())
                .status(veterinary.getStatus())
                .plan(plan)
                .ownerName(veterinary.getLegalRepName())
                .ownerEmail(owner != null ? resolveEmail(owner.getUserId()) : null)
                .createdAt(veterinary.getCreatedAt())
                .approvedAt(veterinary.getApprovedAt())
                .suspendedAt(veterinary.getSuspendedAt())
                .suspensionReason(veterinary.getSuspensionReason())
                .employees(employeeInfos)
                .verificationHistory(history)
                .build();
    }

    private String resolveEmail(UUID userId) {
        try {
            return userServicePort.getUser(userId).email();
        } catch (RuntimeException ex) {
            log.warn("User service lookup failed for userId={}, owner email omitted from admin clinic detail", userId);
            return null;
        }
    }
}
