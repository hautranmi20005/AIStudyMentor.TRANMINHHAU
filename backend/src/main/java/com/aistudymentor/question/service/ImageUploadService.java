package com.aistudymentor.question.service;

import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.question.dto.UploadAuthorizeRequest;
import com.aistudymentor.question.dto.UploadAuthorizeResponse;
import com.aistudymentor.question.entity.ImageUpload;
import com.aistudymentor.question.entity.Question;
import com.aistudymentor.question.entity.QuestionAttachment;
import com.aistudymentor.question.repository.ImageUploadRepository;
import com.aistudymentor.question.storage.ImageContentValidator;
import com.aistudymentor.question.storage.LocalStorageGateway;
import com.aistudymentor.question.storage.StorageGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
public class ImageUploadService {

    private final ImageUploadRepository uploadRepository;
    private final StorageGateway storage;
    private final Duration uploadTtl;
    private final Duration readUrlTtl;
    private final Duration orphanRetention;
    private final String baseUrl;

    public ImageUploadService(
            ImageUploadRepository uploadRepository,
            StorageGateway storage,
            @Value("${app.storage-config.upload-ttl:10m}") Duration uploadTtl,
            @Value("${app.storage-config.read-url-ttl:5m}") Duration readUrlTtl,
            @Value("${app.storage-config.orphan-retention:24h}") Duration orphanRetention,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.uploadRepository = uploadRepository;
        this.storage = storage;
        this.uploadTtl = uploadTtl;
        this.readUrlTtl = readUrlTtl;
        this.orphanRetention = orphanRetention;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public UploadAuthorizeResponse authorize(StudentProfile profile, UploadAuthorizeRequest request) {
        String contentType = request.getContentType();
        long sizeBytes = request.getSizeBytes();
        if (!ImageContentValidator.isAllowed(contentType)) {
            throw invalidImage("IMAGE_TYPE_NOT_ALLOWED", "This image format is not supported.");
        }
        if (sizeBytes < 1 || sizeBytes > ImageContentValidator.MAX_IMAGE_BYTES) {
            throw invalidImage("IMAGE_SIZE_INVALID", "Images must be between 1 byte and 10 MB.");
        }
        if (!StringUtils.hasText(request.getFileName()) || request.getFileName().length() > 255) {
            throw invalidImage("IMAGE_FILE_NAME_INVALID", "The selected image file name is invalid.");
        }

        String storagePath = "users/" + profile.getUser().getUserId() + "/images/"
                + UUID.randomUUID() + "." + ImageContentValidator.extensionFor(contentType);
        Instant expiresAt = Instant.now().plus(uploadTtl);
        ImageUpload saved = uploadRepository.save(ImageUpload.builder()
                .profile(profile)
                .storagePath(storagePath)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .expiresAt(expiresAt)
                .build());
        StorageGateway.UploadGrant grant = new StorageGateway.UploadGrant(
                saved.getUploadId(), profile.getUser().getUserId(), storagePath, contentType, sizeBytes, expiresAt);
        String uploadUrl = storage.createUploadUrl(grant).toString();
        return UploadAuthorizeResponse.builder()
                .uploadId(saved.getUploadId())
                .uploadUrl(uploadUrl)
                .storagePath(storagePath)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public void acceptLocalUpload(
            UUID uploadId,
            String token,
            String contentType,
            long contentLength,
            InputStream input) {
        if (!storage.isLocal()) {
            throw notFound();
        }
        ImageUpload upload = uploadRepository.findById(uploadId).orElseThrow(ImageUploadService::notFound);
        storage.validateUploadToken(grant(upload), token);
        ensureWritable(upload, contentType, contentLength);
        byte[] bytes;
        try {
            bytes = input.readNBytes((int) ImageContentValidator.MAX_IMAGE_BYTES + 1);
        } catch (IOException exception) {
            throw storageUnavailable(exception);
        }
        if (bytes.length != contentLength || bytes.length != upload.getSizeBytes()) {
            throw invalidImage("IMAGE_SIZE_MISMATCH", "The uploaded image size did not match its authorization.");
        }
        validateImage(upload.getContentType(), bytes);
        try {
            storage.write(upload.getStoragePath(), upload.getContentType(), bytes);
        } catch (IOException exception) {
            throw storageUnavailable(exception);
        }
    }

    @Transactional(readOnly = true)
    public StorageGateway.StoredImage readAuthorizedUpload(UUID uploadId, String token) {
        if (!storage.isLocal()) {
            throw notFound();
        }
        ImageUpload upload = uploadRepository.findById(uploadId).orElseThrow(ImageUploadService::notFound);
        if (upload.getExpiresAt().isBefore(Instant.now())) {
            throw expired();
        }
        storage.validateUploadToken(grant(upload), token);
        return readAndValidate(upload.getStoragePath(), upload.getContentType(), upload.getSizeBytes());
    }

    @Transactional(readOnly = true)
    public UploadedImage loadForQuestion(StudentProfile profile, UUID uploadId) {
        ImageUpload upload = uploadRepository.findByUploadIdAndProfile(uploadId, profile)
                .orElseThrow(ImageUploadService::notFound);
        if (upload.getAttachedAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "IMAGE_UPLOAD_ALREADY_USED",
                    "This image upload has already been used.");
        }
        if (upload.getExpiresAt().isBefore(Instant.now())) {
            throw expired();
        }
        StorageGateway.StoredImage stored = readAndValidate(
                upload.getStoragePath(), upload.getContentType(), upload.getSizeBytes());
        return new UploadedImage(upload.getUploadId(), upload.getStoragePath(), stored.contentType(),
                stored.sizeBytes(), sha256(stored.bytes()), stored.bytes());
    }

    @Transactional(readOnly = true)
    public StorageGateway.StoredImage readAttachment(QuestionAttachment attachment) {
        StorageGateway.StoredImage stored = readAndValidate(
                attachment.getFirebaseStoragePath(), attachment.getMimeType(), attachment.getSizeBytes());
        if (!sha256(stored.bytes()).equals(attachment.getSha256())) {
            throw new BusinessException(HttpStatus.GONE, "IMAGE_ATTACHMENT_CORRUPTED",
                    "The stored image no longer matches this question attachment.");
        }
        return stored;
    }

    @Transactional
    public QuestionAttachment attachToQuestion(
            StudentProfile profile,
            UploadedImage image,
            Question question) {
        ImageUpload upload = uploadRepository.findForUpdate(image.uploadId(), profile)
                .orElseThrow(ImageUploadService::notFound);
        if (upload.getAttachedAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "IMAGE_UPLOAD_ALREADY_USED",
                    "This image upload has already been used.");
        }
        if (upload.getExpiresAt().isBefore(Instant.now())
                || !upload.getStoragePath().equals(image.storagePath())
                || !upload.getContentType().equals(image.contentType())
                || upload.getSizeBytes() != image.sizeBytes()) {
            throw expired();
        }
        upload.setAttachedAt(Instant.now());
        uploadRepository.save(upload);
        return QuestionAttachment.builder()
                .question(question)
                .firebaseStoragePath(image.storagePath())
                .mimeType(image.contentType())
                .sizeBytes(image.sizeBytes())
                .sha256(image.sha256())
                .build();
    }

    public String createReadUrl(StudentProfile profile, QuestionAttachment attachment) {
        Instant expiresAt = Instant.now().plus(readUrlTtl);
        StorageGateway.ReadGrant grant = new StorageGateway.ReadGrant(
                profile.getUser().getUserId(), attachment.getFirebaseStoragePath(),
                attachment.getMimeType(), expiresAt);
        return storage.createReadUrl(grant).toString();
    }

    public StorageGateway.StoredImage readSignedImage(
            UUID userId,
            String storagePath,
            String contentType,
            long expiresAtMillis,
            String token) {
        if (!storage.isLocal()) {
            throw notFound();
        }
        if (!storagePath.startsWith("users/" + userId + "/images/")) {
            throw notFound();
        }
        StorageGateway.ReadGrant grant = new StorageGateway.ReadGrant(
                userId, storagePath, contentType, Instant.ofEpochMilli(expiresAtMillis));
        storage.validateReadToken(grant, token);
        return readAndValidate(storagePath, contentType, -1);
    }

    @Scheduled(fixedDelayString = "${app.storage.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredUploads() {
        LocalDateTime cutoff = LocalDateTime.ofInstant(Instant.now().minus(orphanRetention), ZoneOffset.UTC);
        for (ImageUpload upload : uploadRepository.findTop100ByCreatedAtBeforeOrderByCreatedAtAsc(cutoff)) {
            if (upload.getAttachedAt() == null) {
                try {
                    storage.delete(upload.getStoragePath());
                } catch (IOException exception) {
                    log.warn("Could not clean up expired image upload {}", upload.getUploadId());
                    continue;
                }
            }
            uploadRepository.delete(upload);
        }
    }

    private void ensureWritable(ImageUpload upload, String contentType, long contentLength) {
        if (upload.getAttachedAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "IMAGE_UPLOAD_ALREADY_USED",
                    "This image upload has already been used.");
        }
        if (upload.getExpiresAt().isBefore(Instant.now())) {
            throw expired();
        }
        if (!upload.getContentType().equals(contentType)) {
            throw invalidImage("IMAGE_CONTENT_TYPE_MISMATCH",
                    "The uploaded image content type did not match its authorization.");
        }
        if (contentLength != upload.getSizeBytes()
                || contentLength < 1 || contentLength > ImageContentValidator.MAX_IMAGE_BYTES) {
            throw invalidImage("IMAGE_SIZE_MISMATCH", "The uploaded image size did not match its authorization.");
        }
    }

    private StorageGateway.StoredImage readAndValidate(String path, String contentType, long expectedSize) {
        try {
            StorageGateway.StoredImage stored = storage.read(path);
            if ((expectedSize > 0 && stored.sizeBytes() != expectedSize)
                    || !contentType.equals(stored.contentType())) {
                throw invalidImage("IMAGE_METADATA_MISMATCH",
                        "The stored image did not match its authorized size and content type.");
            }
            validateImage(contentType, stored.bytes());
            return stored;
        } catch (java.nio.file.NoSuchFileException exception) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "IMAGE_UPLOAD_NOT_FOUND",
                    "The uploaded image could not be found.");
        } catch (IOException exception) {
            throw storageUnavailable(exception);
        }
    }

    private StorageGateway.UploadGrant grant(ImageUpload upload) {
        return new StorageGateway.UploadGrant(upload.getUploadId(), upload.getProfile().getUser().getUserId(),
                upload.getStoragePath(), upload.getContentType(), upload.getSizeBytes(), upload.getExpiresAt());
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private void validateImage(String contentType, byte[] bytes) {
        try {
            ImageContentValidator.validate(contentType, bytes);
        } catch (ImageContentValidator.InvalidImageException exception) {
            throw invalidImage(exception.code(), "The file is not a valid supported image.");
        }
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "IMAGE_UPLOAD_NOT_FOUND",
                "The uploaded image was not found or does not belong to you.");
    }

    private static BusinessException expired() {
        return new BusinessException(HttpStatus.GONE, "IMAGE_UPLOAD_EXPIRED",
                "The image upload authorization has expired. Please select the image again.");
    }

    private static BusinessException invalidImage(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    private static BusinessException storageUnavailable(IOException cause) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE_STORAGE_UNAVAILABLE",
                "Image storage is temporarily unavailable.");
    }

    public record UploadedImage(
            UUID uploadId,
            String storagePath,
            String contentType,
            long sizeBytes,
            String sha256,
            byte[] bytes) {
    }
}
