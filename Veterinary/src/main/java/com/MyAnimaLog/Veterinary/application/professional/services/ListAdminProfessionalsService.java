package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalListItem;
import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalListResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.ListAdminProfessionalsUseCase;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminWorkplaceInfo;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Like {@code ListAdminClinicsService}, makes zero calls to {@code UserServicePort}: the professional
 * profile doesn't store an email (it lives only in the User service, with no batch lookup endpoint
 * today), so this list never shows or searches by email — only {@code GetAdminProfessionalDetailService}
 * resolves it, for a single profile.
 */
@Service
@RequiredArgsConstructor
public class ListAdminProfessionalsService implements ListAdminProfessionalsUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final VeterinaryAuthorizationService authorizationService;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Override
    public AdminProfessionalListResponse list(String search, Boolean verified, Integer page, Integer size) {
        authorizationService.requirePlatformAdmin();

        int effectivePage = page != null ? page : 0;
        int effectiveSize = size != null ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;

        ProfessionalProfileRepositoryPort.AdminProfessionalPage result =
                professionalProfileRepositoryPort.findForAdminList(verified, search, effectivePage, effectiveSize);

        List<UUID> userIds = result.items().stream().map(ProfessionalProfile::getUserId).toList();

        List<VeterinaryEmployee> employees = employeeRepositoryPort.findByUserIdInAndActiveTrue(userIds);
        Map<UUID, List<VeterinaryEmployee>> employeesByUserId = employees.stream()
                .collect(Collectors.groupingBy(VeterinaryEmployee::getUserId));

        List<UUID> veterinaryIds = employees.stream().map(VeterinaryEmployee::getVeterinaryId).distinct().toList();
        Map<UUID, String> clinicNameById = veterinaryRepositoryPort.findByIdIn(veterinaryIds).stream()
                .collect(Collectors.toMap(Veterinary::getId, Veterinary::getName));

        List<AdminProfessionalListItem> items = result.items().stream()
                .map(profile -> toListItem(profile, employeesByUserId.getOrDefault(profile.getUserId(), List.of()), clinicNameById))
                .toList();

        return AdminProfessionalListResponse.builder()
                .items(items)
                .page(effectivePage)
                .size(effectiveSize)
                .total(result.total())
                .build();
    }

    private AdminProfessionalListItem toListItem(ProfessionalProfile profile, List<VeterinaryEmployee> employees,
                                                   Map<UUID, String> clinicNameById) {
        List<AdminWorkplaceInfo> workplaces = employees.stream()
                .map(e -> AdminWorkplaceInfo.builder()
                        .veterinaryId(e.getVeterinaryId())
                        .name(clinicNameById.get(e.getVeterinaryId()))
                        .role(e.getRole())
                        .build())
                .toList();

        return AdminProfessionalListItem.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .licenseNumber(profile.getLicenseNumber())
                .profession(profile.getProfession())
                .licenseStatus(profile.getLicenseStatus())
                .workplaces(workplaces)
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
