package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.CreateEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.dto.CreateEmployeeResponse;

public interface CreateEmployeeUseCase {
    CreateEmployeeResponse create(CreateEmployeeRequest request);
}
