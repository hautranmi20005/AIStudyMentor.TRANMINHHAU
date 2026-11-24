package com.aistudymentor.question.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalStorageGatewayTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void uploadAuthorizationBindsTheUserPathExpiryTypeAndSize() {
        LocalStorageGateway gateway = new LocalStorageGateway(
                temporaryDirectory.toString(), "http://localhost:8080", "0123456789abcdef0123456789abcdef");
        UUID userId = UUID.randomUUID();
        StorageGateway.UploadGrant grant = new StorageGateway.UploadGrant(
                UUID.randomUUID(), userId, "users/" + userId + "/images/" + UUID.randomUUID() + ".png",
                "image/png", 8, Instant.now().plusSeconds(600));
        String token = UriComponentsBuilder.fromUri(gateway.createUploadUrl(grant))
                .build().getQueryParams().getFirst("token");

        assertDoesNotThrow(() -> gateway.validateUploadToken(grant, token));
        assertThrows(LocalStorageGateway.InvalidStorageTokenException.class,
                () -> gateway.validateUploadToken(new StorageGateway.UploadGrant(
                        grant.uploadId(), grant.userId(), grant.storagePath(), "image/jpeg",
                        grant.sizeBytes(), grant.expiresAt()), token));
        assertThrows(LocalStorageGateway.InvalidStorageTokenException.class,
                () -> gateway.validateUploadToken(new StorageGateway.UploadGrant(
                        grant.uploadId(), UUID.randomUUID(), grant.storagePath(), grant.contentType(),
                        grant.sizeBytes(), grant.expiresAt()), token));
    }

    @Test
    void localUploadAndReadUrlsExpireAndProtectStoredBytes() throws Exception {
        LocalStorageGateway gateway = new LocalStorageGateway(
                temporaryDirectory.toString(), "http://localhost:8080", "0123456789abcdef0123456789abcdef");
        UUID userId = UUID.randomUUID();
        String path = "users/" + userId + "/images/" + UUID.randomUUID() + ".png";
        byte[] image = new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        gateway.write(path, "image/png", image);
        StorageGateway.ReadGrant grant = new StorageGateway.ReadGrant(
                userId, path, "image/png", Instant.now().plusSeconds(60));
        String token = UriComponentsBuilder.fromUri(gateway.createReadUrl(grant))
                .build().getQueryParams().getFirst("token");

        assertDoesNotThrow(() -> gateway.validateReadToken(grant, token));
        assertThrows(LocalStorageGateway.InvalidStorageTokenException.class,
                () -> gateway.validateReadToken(new StorageGateway.ReadGrant(
                        userId, path, "image/png", Instant.now().minusSeconds(1)), token));
    }
}
