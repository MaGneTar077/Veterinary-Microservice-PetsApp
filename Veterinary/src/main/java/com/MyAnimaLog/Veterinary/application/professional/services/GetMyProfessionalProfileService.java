package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetMyProfessionalProfileUseCase;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMyProfessionalProfileService implements GetMyProfessionalProfileUseCase {

    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    public ProfessionalProfileResponse getMyProfile() {
        AuthenticatedUser caller = authenticatedUserPort.current();
        ProfessionalProfile profile = professionalProfileRepositoryPort.findByUserId(caller.userId())
                .orElseThrow(ProfessionalProfileNotFoundException::new);

        return toResponse(profile);
    }

    static ProfessionalProfileResponse toResponse(ProfessionalProfile profile) {
        return ProfessionalProfileResponse.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .documentType(profile.getDocumentType())
                .documentNumber(profile.getDocumentNumber())
                .profession(profile.getProfession())
                .licenseNumber(profile.getLicenseNumber())
                .specialty(profile.getSpecialty())
                .university(profile.getUniversity())
                .licenseStatus(profile.getLicenseStatus())
                .build();
    }
}
