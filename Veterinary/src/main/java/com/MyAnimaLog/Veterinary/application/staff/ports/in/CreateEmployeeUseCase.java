package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.CreateEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.CreateEmployeeResponse;

public interface CreateEmployeeUseCase {
    CreateEmployeeResponse create(CreateEmployeeRequest request);
}
