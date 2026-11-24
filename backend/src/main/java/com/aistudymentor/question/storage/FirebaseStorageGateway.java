package com.aistudymentor.question.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "firebase")
public class FirebaseStorageGateway implements StorageGateway {

    private final Storage storage;
    private final String bucket;

    public FirebaseStorageGateway(
            Storage storage,
            @org.springframework.beans.factory.annotation.Value("${app.storage-config.firebase.bucket}") String bucket) {
        this.storage = storage;
        this.bucket = bucket;
    }

    @Override
    public URI createUploadUrl(UploadGrant grant) {
        return URI.create(storage.signUrl(blobInfo(grant.storagePath(), grant.contentType()),
                remainingSeconds(grant.expiresAt()), TimeUnit.SECONDS,
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withContentType()).toString());
    }

    @Override
    public URI createReadUrl(ReadGrant grant) {
        return URI.create(storage.signUrl(blobInfo(grant.storagePath(), grant.contentType()),
                remainingSeconds(grant.expiresAt()), TimeUnit.SECONDS,
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.httpMethod(HttpMethod.GET)).toString());
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public void validateUploadToken(UploadGrant grant, String token) {
        throw new UnsupportedOperationException("Firebase upload URLs are validated by their signed URL.");
    }

    @Override
    public void validateReadToken(ReadGrant grant, String token) {
        throw new UnsupportedOperationException("Firebase read URLs are validated by their signed URL.");
    }

    @Override
    public void write(String storagePath, String contentType, byte[] bytes) {
        storage.create(blobInfo(storagePath, contentType), bytes);
    }

    @Override
    public StoredImage read(String storagePath) throws IOException {
        Blob blob = storage.get(bucket, storagePath);
        if (blob == null) {
            throw new java.nio.file.NoSuchFileException(storagePath);
        }
        Long size = blob.getSize();
        if (size == null || size < 1 || size > ImageContentValidator.MAX_IMAGE_BYTES) {
            throw new ImageContentValidator.InvalidImageException("IMAGE_SIZE_INVALID");
        }
        byte[] bytes = blob.getContent();
        return new StoredImage(bytes, blob.getContentType());
    }

    @Override
    public void delete(String storagePath) {
        storage.delete(bucket, storagePath);
    }

    private BlobInfo blobInfo(String path, String contentType) {
        return BlobInfo.newBuilder(bucket, path).setContentType(contentType).build();
    }

    private long remainingSeconds(java.time.Instant expiresAt) {
        long seconds = java.time.Duration.between(java.time.Instant.now(), expiresAt).toSeconds();
        if (seconds < 1) {
            throw new IllegalArgumentException("Image authorization has expired.");
        }
        return seconds;
    }
}
