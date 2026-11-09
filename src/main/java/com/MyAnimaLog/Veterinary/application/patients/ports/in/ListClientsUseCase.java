package com.MyAnimaLog.Veterinary.application.patients.ports.in;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListClientsResponse;

import java.util.UUID;

public interface ListClientsUseCase {
    ListClientsResponse list(UUID veterinaryId, Integer page, Integer size);
}
