package com.MyAnimaLog.Veterinary.infrastructure.storage;

import java.util.Optional;

/**
 * Detects a file's real type from its leading bytes (magic numbers), never from a client-supplied
 * Content-Type header — used by endpoints that must reject a renamed/mislabeled upload.
 */
public final class FileSignatureDetector {

    private FileSignatureDetector() {
    }

    public enum DetectedType {
        PNG("png", "image/png"),
        JPEG("jpg", "image/jpeg"),
        WEBP("webp", "image/webp"),
        PDF("pdf", "application/pdf");

        private final String extension;
        private final String mimeType;

        DetectedType(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public String extension() {
            return extension;
        }

        public String mimeType() {
            return mimeType;
        }
    }

    public static Optional<DetectedType> detect(byte[] bytes) {
        if (bytes == null) {
            return Optional.empty();
        }
        if (matches(bytes, 0, (byte) 0x89, 'P', 'N', 'G')) {
            return Optional.of(DetectedType.PNG);
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return Optional.of(DetectedType.JPEG);
        }
        if (bytes.length >= 12
                && matches(bytes, 0, 'R', 'I', 'F', 'F')
                && matches(bytes, 8, 'W', 'E', 'B', 'P')) {
            return Optional.of(DetectedType.WEBP);
        }
        if (matches(bytes, 0, '%', 'P', 'D', 'F')) {
            return Optional.of(DetectedType.PDF);
        }
        return Optional.empty();
    }

    private static boolean matches(byte[] bytes, int offset, int... expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[offset + i] & 0xFF) != (expected[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
