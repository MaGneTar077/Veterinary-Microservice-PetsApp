package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListVeterinaryPatientsResponse;

import java.util.UUID;

public interface ListVeterinaryPatientsUseCase {
    ListVeterinaryPatientsResponse list(UUID veterinaryId, Integer page, Integer size);
}
