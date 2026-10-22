package com.MyAnimaLog.Veterinary.infrastructure.storage;

/**
 * Strips any path information from a client-supplied original file name (never trust it as a
 * path — some browsers/clients send the full path) and caps it to fit {@code VARCHAR(255)}.
 */
public final class FileNameSanitizer {

    private static final int MAX_LENGTH = 255;

    private FileNameSanitizer() {
    }

    public static String sanitize(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return null;
        }
        String withoutPath = originalFileName.replace('\\', '/');
        int lastSlash = withoutPath.lastIndexOf('/');
        String name = lastSlash >= 0 ? withoutPath.substring(lastSlash + 1) : withoutPath;
        name = name.trim();
        if (name.isEmpty()) {
            return null;
        }
        return name.length() > MAX_LENGTH ? name.substring(0, MAX_LENGTH) : name;
    }
}
