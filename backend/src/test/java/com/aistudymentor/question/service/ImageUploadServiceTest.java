package com.aistudymentor.question.service;

import com.aistudymentor.auth.entity.User;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.question.dto.UploadAuthorizeRequest;
import com.aistudymentor.question.entity.ImageUpload;
import com.aistudymentor.question.repository.ImageUploadRepository;
import com.aistudymentor.question.storage.StorageGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageUploadServiceTest {

    private ImageUploadRepository uploads;
    private StorageGateway storage;
    private ImageUploadService service;

    @BeforeEach
    void setUp() {
        uploads = mock(ImageUploadRepository.class);
        storage = mock(StorageGateway.class);
        service = new ImageUploadService(uploads, storage,
                java.time.Duration.ofMinutes(10), java.time.Duration.ofMinutes(5),
                java.time.Duration.ofHours(24), "http://localhost:8080");
    }

    @Test
    void authorizationCreatesGeneratedUserScopedPathAndEnforcesMimetypeAndSize() {
        StudentProfile owner = profile(UUID.randomUUID());
        when(storage.createUploadUrl(any())).thenReturn(java.net.URI.create("https://storage.example/upload"));
        when(uploads.save(any())).thenAnswer(invocation -> {
            ImageUpload saved = invocation.getArgument(0);
            saved.setUploadId(UUID.randomUUID());
            return saved;
        });

        UploadAuthorizeRequest request = new UploadAuthorizeRequest("not-a-path.pdf", "image/png", 8);
        var response = service.authorize(owner, request);

        org.junit.jupiter.api.Assertions.assertNotNull(response.getUploadId());
        assertEquals(owner.getUser().getUserId().toString(),
                response.getStoragePath().split("/")[1]);
        assertEquals("image/png", response.getStoragePath().endsWith(".png") ? "image/png" : "invalid");

        request.setContentType("application/pdf");
        assertThrows(BusinessException.class, () -> service.authorize(owner, request));
        request.setContentType("image/png");
        request.setSizeBytes(10L * 1024 * 1024 + 1);
        assertThrows(BusinessException.class, () -> service.authorize(owner, request));
    }

    @Test
    void rejectsAnotherUsersOrExpiredUploadBeforeReadingStorage() throws Exception {
        StudentProfile owner = profile(UUID.randomUUID());
        UUID uploadId = UUID.randomUUID();
        when(uploads.findByUploadIdAndProfile(uploadId, owner)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.loadForQuestion(owner, uploadId)).getStatus().value());
        verify(storage, never()).read(any());

        ImageUpload expired = uploadsEntity(uploadId, owner, "image/png", 8);
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(uploads.findByUploadIdAndProfile(uploadId, owner)).thenReturn(Optional.of(expired));
        assertEquals(410, assertThrows(BusinessException.class,
                () -> service.loadForQuestion(owner, uploadId)).getStatus().value());
        verify(storage, never()).read(any());
    }

    @Test
    void cleanupDeletesOnlyUnattachedObjectsAndRetainsRowsWhenStorageDeleteFails() throws Exception {
        StudentProfile owner = profile(UUID.randomUUID());
        ImageUpload orphan = uploadsEntity(UUID.randomUUID(), owner, "image/png", 8);
        orphan.setCreatedAt(LocalDateTime.now().minusDays(2));
        ImageUpload storedAttachment = uploadsEntity(UUID.randomUUID(), owner, "image/png", 8);
        storedAttachment.setCreatedAt(LocalDateTime.now().minusDays(2));
        storedAttachment.setAttachedAt(Instant.now().minusSeconds(1));
        when(uploads.findTop100ByCreatedAtBeforeOrderByCreatedAtAsc(any()))
                .thenReturn(java.util.List.of(orphan, storedAttachment));
        org.mockito.Mockito.doThrow(new IOException("storage unavailable"))
                .when(storage).delete(orphan.getStoragePath());

        service.cleanupExpiredUploads();

        verify(uploads, never()).delete(orphan);
        verify(storage, never()).delete(storedAttachment.getStoragePath());
        verify(uploads).delete(storedAttachment);
    }

    private StudentProfile profile(UUID userId) {
        return StudentProfile.builder()
                .profileId(UUID.randomUUID())
                .user(User.builder().userId(userId).build())
                .build();
    }

    private ImageUpload uploadsEntity(UUID id, StudentProfile profile, String type, long size) {
        return ImageUpload.builder()
                .uploadId(id)
                .profile(profile)
                .storagePath("users/" + profile.getUser().getUserId() + "/images/" + id + ".png")
                .contentType(type)
                .sizeBytes(size)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
    }
}
