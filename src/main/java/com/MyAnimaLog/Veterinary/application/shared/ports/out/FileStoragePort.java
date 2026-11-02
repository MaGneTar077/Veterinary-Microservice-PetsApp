package com.MyAnimaLog.Veterinary.application.shared.ports.out;

import java.time.Duration;

public interface FileStoragePort {

    String uploadPublic(String bucket, String path, byte[] bytes, String contentType);

    void uploadPrivate(String bucket, String path, byte[] bytes, String contentType);

    String createSignedUrl(String bucket, String path, Duration ttl);

    void delete(String bucket, String path);
}
