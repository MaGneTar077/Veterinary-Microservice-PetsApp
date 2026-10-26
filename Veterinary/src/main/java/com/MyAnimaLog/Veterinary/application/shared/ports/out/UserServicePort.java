package com.MyAnimaLog.Veterinary.application.shared.ports.out;

import java.util.UUID;

public interface UserServicePort {
    UserInfo getUser(UUID userId);

    record UserInfo(UUID id, String email, Boolean emailVerified) {
    }
}
