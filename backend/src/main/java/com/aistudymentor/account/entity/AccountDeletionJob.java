package com.aistudymentor.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_deletion_jobs")
@Getter
@Setter
@NoArgsConstructor
public class AccountDeletionJob {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "profile_id", nullable = false, length = 36)
    private String profileId;

    @Column(name = "user_id_hash", nullable = false, length = 64)
    private String userIdHash;

    @Lob
    @Column(name = "storage_paths_json", nullable = false, columnDefinition = "TEXT")
    private String storagePathsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
