package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.SubmitProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.SubmitProfessionalProfileUseCase;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitProfessionalProfileService implements SubmitProfessionalProfileUseCase {

    private final ProfessionalProfileSubmissionService professionalProfileSubmissionService;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    @Transactional
    public SubmitProfessionalProfileResponse submit() {
        AuthenticatedUser caller = authenticatedUserPort.current();
        return professionalProfileSubmissionService.submitForUser(caller.userId());
    }
}
