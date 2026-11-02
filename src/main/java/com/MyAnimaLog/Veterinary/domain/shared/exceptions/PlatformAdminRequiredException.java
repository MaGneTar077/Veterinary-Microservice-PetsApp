package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

public class PlatformAdminRequiredException extends RuntimeException {

    public PlatformAdminRequiredException() {
        super("This operation requires platform_role=PLATFORM_ADMIN");
    }
}
