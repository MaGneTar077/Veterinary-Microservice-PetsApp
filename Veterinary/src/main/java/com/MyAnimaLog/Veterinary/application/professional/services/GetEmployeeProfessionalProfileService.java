package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.EmployeeProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetEmployeeProfessionalProfileUseCase;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetEmployeeProfessionalProfileService implements GetEmployeeProfessionalProfileUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public EmployeeProfessionalProfileResponse getProfile(UUID veterinaryId, UUID employeeId) {
        authorizationService.requireMember(veterinaryId);

        VeterinaryEmployee employee = employeeRepositoryPort.findById(employeeId)
                .filter(e -> e.getVeterinaryId().equals(veterinaryId))
                .orElseThrow(EmployeeNotFoundException::new);

        Optional<ProfessionalProfile> profile = professionalProfileRepositoryPort.findByUserId(employee.getUserId());

        return EmployeeProfessionalProfileResponse.builder()
                .exists(profile.isPresent())
                .fullName(profile.map(ProfessionalProfile::getFullName).orElse(null))
                .profession(profile.map(ProfessionalProfile::getProfession).orElse(null))
                .licenseNumber(profile.map(ProfessionalProfile::getLicenseNumber).orElse(null))
                .specialty(profile.map(ProfessionalProfile::getSpecialty).orElse(null))
                .university(profile.map(ProfessionalProfile::getUniversity).orElse(null))
                .licenseStatus(profile.map(ProfessionalProfile::getLicenseStatus).orElse(null))
                .build();
    }
}
