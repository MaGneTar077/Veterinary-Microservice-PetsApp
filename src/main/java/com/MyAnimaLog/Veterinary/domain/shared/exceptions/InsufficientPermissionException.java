package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

import com.MyAnimaLog.Veterinary.domain.security.Permission;

public class InsufficientPermissionException extends RuntimeException {

    public InsufficientPermissionException(Permission permission) {
        super("Missing required permission: " + permission);
    }
}
