package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalDetailResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetAdminProfessionalDetailUseCase;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.dto.AdminWorkplaceInfo;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAdminProfessionalDetailService implements GetAdminProfessionalDetailUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetAdminProfessionalDetailService.class);

    private final VeterinaryAuthorizationService authorizationService;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final UserServicePort userServicePort;

    @Override
    public AdminProfessionalDetailResponse getDetail(UUID profileId) {
        authorizationService.requirePlatformAdmin();

        ProfessionalProfile profile = professionalProfileRepositoryPort.findById(profileId)
                .orElseThrow(ProfessionalProfileNotFoundException::new);

        List<AdminWorkplaceInfo> workplaces = employeeRepositoryPort.findByUserIdAndActiveTrue(profile.getUserId()).stream()
                .map(employee -> AdminWorkplaceInfo.builder()
                        .veterinaryId(employee.getVeterinaryId())
                        .name(veterinaryRepositoryPort.findById(employee.getVeterinaryId()).map(Veterinary::getName).orElse(null))
                        .role(employee.getRole())
                        .build())
                .toList();

        return AdminProfessionalDetailResponse.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .email(resolveEmail(profile.getUserId()))
                .documentType(profile.getDocumentType())
                .documentNumber(profile.getDocumentNumber())
                .licenseNumber(profile.getLicenseNumber())
                .profession(profile.getProfession())
                .specialty(profile.getSpecialty())
                .university(profile.getUniversity())
                .licenseStatus(profile.getLicenseStatus())
                .rejectionReason(profile.getRejectionReason())
                .verifiedAt(profile.getVerifiedAt())
                .workplaces(workplaces)
                .createdAt(profile.getCreatedAt())
                .build();
    }

    private String resolveEmail(UUID userId) {
        try {
            return userServicePort.getUser(userId).email();
        } catch (RuntimeException ex) {
            log.warn("User service lookup failed for userId={}, email omitted from admin professional detail", userId);
            return null;
        }
    }
}
