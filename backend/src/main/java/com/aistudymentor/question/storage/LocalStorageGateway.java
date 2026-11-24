package com.aistudymentor.question.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "local", matchIfMissing = true)
public class LocalStorageGateway implements StorageGateway {

    private final Path root;
    private final String baseUrl;
    private final byte[] signingSecret;

    public LocalStorageGateway(
            @Value("${app.storage-config.local.directory:.local-storage}") String directory,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.storage-config.signing-secret:${app.jwt.secret:}}") String signingSecret) {
        if (signingSecret.length() < 32) {
            throw new IllegalStateException("app.storage.signing-secret must contain at least 32 characters.");
        }
        this.root = Path.of(directory).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.signingSecret = signingSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Local image storage directory could not be created.", exception);
        }
    }

    @Override
    public java.net.URI createUploadUrl(UploadGrant grant) {
        String token = sign(uploadTokenPayload(grant));
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/v1/uploads/{uploadId}/content")
                .queryParam("token", token)
                .buildAndExpand(grant.uploadId())
                .encode()
                .toUri();
    }

    @Override
    public java.net.URI createReadUrl(ReadGrant grant) {
        String token = sign(readTokenPayload(grant));
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/v1/images/content")
                .queryParam("userId", grant.userId())
                .queryParam("storagePath", grant.storagePath())
                .queryParam("contentType", grant.contentType())
                .queryParam("expiresAt", grant.expiresAt().toEpochMilli())
                .queryParam("token", token)
                .build()
                .encode()
                .toUri();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public void validateUploadToken(UploadGrant grant, String token) {
        requireValidToken(uploadTokenPayload(grant), token);
    }

    @Override
    public void validateReadToken(ReadGrant grant, String token) {
        if (grant.expiresAt().isBefore(java.time.Instant.now())) {
            throw new InvalidStorageTokenException();
        }
        requireValidToken(readTokenPayload(grant), token);
    }

    @Override
    public void write(String storagePath, String contentType, byte[] bytes) throws IOException {
        Path target = resolve(storagePath);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public StoredImage read(String storagePath) throws IOException {
        Path target = resolve(storagePath);
        if (!Files.isRegularFile(target)) {
            throw new java.nio.file.NoSuchFileException(storagePath);
        }
        long size = Files.size(target);
        if (size < 1 || size > ImageContentValidator.MAX_IMAGE_BYTES) {
            throw new ImageContentValidator.InvalidImageException("IMAGE_SIZE_INVALID");
        }
        byte[] bytes = Files.readAllBytes(target);
        return new StoredImage(bytes, contentTypeFor(storagePath));
    }

    @Override
    public void delete(String storagePath) throws IOException {
        Files.deleteIfExists(resolve(storagePath));
    }

    private Path resolve(String storagePath) {
        Path resolved = root.resolve(storagePath).normalize();
        if (!resolved.startsWith(root) || !storagePath.matches("users/[0-9a-fA-F-]{36}/images/[0-9a-fA-F-]{36}\\.(jpg|png|webp|heic|heif)")) {
            throw new IllegalArgumentException("Invalid private image storage path.");
        }
        return resolved;
    }

    private String contentTypeFor(String storagePath) {
        return switch (storagePath.substring(storagePath.lastIndexOf('.') + 1)) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            case "heif" -> "image/heif";
            default -> throw new IllegalArgumentException("Unsupported image storage extension.");
        };
    }

    private String uploadTokenPayload(UploadGrant grant) {
        return grant.userId() + "\n" + grant.uploadId() + "\n" + grant.storagePath()
                + "\n" + grant.contentType() + "\n" + grant.sizeBytes() + "\n" + grant.expiresAt().toEpochMilli();
    }

    private String readTokenPayload(ReadGrant grant) {
        return grant.userId() + "\n" + grant.storagePath()
                + "\n" + grant.contentType() + "\n" + grant.expiresAt().toEpochMilli();
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign local image authorization.", exception);
        }
    }

    private void requireValidToken(String payload, String token) {
        if (token == null || !MessageDigest.isEqual(
                sign(payload).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                token.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            throw new InvalidStorageTokenException();
        }
    }

    public static final class InvalidStorageTokenException extends AccessDeniedException {
        public InvalidStorageTokenException() {
            super("Invalid image authorization token.");
        }
    }
}
