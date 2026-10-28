package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicListItem;
import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicListResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.ListAdminClinicsUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deliberately makes zero calls to {@code UserServicePort} per row: the User service has no batch
 * lookup endpoint today, so resolving owner name/email here would be N REST calls per page. Those
 * fields live only on {@link GetAdminClinicDetailService} (one clinic, one call); the list shows
 * {@code legalRepName} instead, which is already on the {@code veterinary} row.
 */
@Service
@RequiredArgsConstructor
public class ListAdminClinicsService implements ListAdminClinicsUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final VeterinaryAuthorizationService authorizationService;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;

    @Override
    public AdminClinicListResponse list(VeterinaryStatus status, String search, String plan, String city,
                                         String department, Integer page, Integer size) {
        authorizationService.requirePlatformAdmin();

        int effectivePage = page != null ? page : 0;
        int effectiveSize = size != null ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;

        VeterinaryRepositoryPort.AdminClinicPage result = veterinaryRepositoryPort.findForAdminList(
                status, search, plan, city, department, effectivePage, effectiveSize);

        List<UUID> veterinaryIds = result.items().stream().map(Veterinary::getId).toList();

        Map<UUID, Long> activeEmployeeCountByVeterinary = employeeRepositoryPort
                .findByVeterinaryIdIn(veterinaryIds).stream()
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .collect(Collectors.groupingBy(VeterinaryEmployee::getVeterinaryId, Collectors.counting()));

        Map<UUID, String> planByVeterinary = subscriptionRepositoryPort.findActiveByVeterinaryIdIn(veterinaryIds)
                .stream()
                .collect(Collectors.toMap(VeterinarySubscription::getVeterinaryId,
                        VeterinarySubscription::getPlan, (a, b) -> a));

        List<AdminClinicListItem> items = result.items().stream()
                .map(veterinary -> toListItem(veterinary,
                        activeEmployeeCountByVeterinary.getOrDefault(veterinary.getId(), 0L),
                        planByVeterinary.get(veterinary.getId())))
                .toList();

        return AdminClinicListResponse.builder()
                .items(items)
                .page(effectivePage)
                .size(effectiveSize)
                .total(result.total())
                .build();
    }

    private AdminClinicListItem toListItem(Veterinary veterinary, long activeEmployeesCount, String plan) {
        return AdminClinicListItem.builder()
                .id(veterinary.getId())
                .name(veterinary.getName())
                .legalName(veterinary.getLegalName())
                .nit(veterinary.getNit() != null ? veterinary.getNit().value() : null)
                .city(veterinary.getCity())
                .department(veterinary.getDepartment())
                .status(veterinary.getStatus())
                .plan(plan)
                .legalRepName(veterinary.getLegalRepName())
                .employeesCount((int) activeEmployeesCount)
                .createdAt(veterinary.getCreatedAt())
                .approvedAt(veterinary.getApprovedAt())
                .suspendedAt(veterinary.getSuspendedAt())
                .build();
    }
}
