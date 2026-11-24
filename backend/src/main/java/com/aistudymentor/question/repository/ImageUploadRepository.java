package com.aistudymentor.question.repository;

import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.question.entity.ImageUpload;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageUploadRepository extends JpaRepository<ImageUpload, UUID> {

    Optional<ImageUpload> findByUploadIdAndProfile(UUID uploadId, StudentProfile profile);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select upload from ImageUpload upload where upload.uploadId = :uploadId "
            + "and upload.profile = :profile")
    Optional<ImageUpload> findForUpdate(
            @Param("uploadId") UUID uploadId,
            @Param("profile") StudentProfile profile);

    List<ImageUpload> findTop100ByCreatedAtBeforeOrderByCreatedAtAsc(LocalDateTime cutoff);
}
