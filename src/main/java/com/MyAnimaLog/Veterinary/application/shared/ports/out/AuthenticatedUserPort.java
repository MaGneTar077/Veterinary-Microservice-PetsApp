package com.MyAnimaLog.Veterinary.application.shared.ports.out;

import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;

public interface AuthenticatedUserPort {

    /**
     * @throws com.MyAnimaLog.Veterinary.domain.shared.exceptions.UnauthenticatedException
     *         if there is no authenticated JWT principal in the current request.
     */
    AuthenticatedUser current();
}
