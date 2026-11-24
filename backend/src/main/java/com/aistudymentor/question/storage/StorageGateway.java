package com.aistudymentor.question.storage;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public interface StorageGateway {

    URI createUploadUrl(UploadGrant grant);

    URI createReadUrl(ReadGrant grant);

    boolean isLocal();

    void validateUploadToken(UploadGrant grant, String token);

    void validateReadToken(ReadGrant grant, String token);

    void write(String storagePath, String contentType, byte[] bytes) throws IOException;

    StoredImage read(String storagePath) throws IOException;

    void delete(String storagePath) throws IOException;

    record UploadGrant(
            UUID uploadId,
            UUID userId,
            String storagePath,
            String contentType,
            long sizeBytes,
            Instant expiresAt) {
    }

    record ReadGrant(
            UUID userId,
            String storagePath,
            String contentType,
            Instant expiresAt) {
    }

    record StoredImage(byte[] bytes, String contentType) {
        public long sizeBytes() {
            return bytes.length;
        }
    }
}
