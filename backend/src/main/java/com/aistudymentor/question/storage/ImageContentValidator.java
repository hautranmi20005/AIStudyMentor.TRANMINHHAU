package com.aistudymentor.question.storage;

import java.util.Map;
import java.util.Set;

public final class ImageContentValidator {

    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/heic", "heic",
            "image/heif", "heif");

    private ImageContentValidator() {
    }

    public static String validate(String contentType, byte[] bytes) {
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidImageException("IMAGE_TYPE_NOT_ALLOWED");
        }
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new InvalidImageException("IMAGE_SIZE_INVALID");
        }
        String detected = detect(bytes);
        if (!contentType.equals(detected)) {
            throw new InvalidImageException("IMAGE_CONTENT_TYPE_MISMATCH");
        }
        return detected;
    }

    public static String extensionFor(String contentType) {
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new InvalidImageException("IMAGE_TYPE_NOT_ALLOWED");
        }
        return extension;
    }

    public static boolean isAllowed(String contentType) {
        return ALLOWED_TYPES.contains(contentType);
    }

    private static String detect(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && (bytes[4] & 0xff) == 0x0d
                && (bytes[5] & 0xff) == 0x0a
                && (bytes[6] & 0xff) == 0x1a
                && (bytes[7] & 0xff) == 0x0a) {
            return "image/png";
        }
        if (bytes.length >= 12
                && ascii(bytes, 0, 4).equals("RIFF")
                && ascii(bytes, 8, 4).equals("WEBP")) {
            return "image/webp";
        }
        if (bytes.length >= 12 && ascii(bytes, 4, 4).equals("ftyp")) {
            String brand = ascii(bytes, 8, 4);
            if (Set.of("heic", "heix", "hevc", "hevx").contains(brand)) {
                return "image/heic";
            }
            if (Set.of("mif1", "msf1").contains(brand)) {
                return "image/heif";
            }
        }
        throw new InvalidImageException("IMAGE_MAGIC_BYTES_INVALID");
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, java.nio.charset.StandardCharsets.US_ASCII);
    }

    public static final class InvalidImageException extends RuntimeException {
        private final String code;

        public InvalidImageException(String code) {
            super(code);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
