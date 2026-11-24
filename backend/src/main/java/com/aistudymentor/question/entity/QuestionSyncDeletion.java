package com.aistudymentor.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "question_sync_deletions")
@Getter
@Setter
@NoArgsConstructor
public class QuestionSyncDeletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "deletion_id", updatable = false, nullable = false, length = 36)
    private UUID deletionId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "profile_id", nullable = false, length = 36)
    private UUID profileId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "question_id", nullable = false, length = 36)
    private UUID questionId;

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    public QuestionSyncDeletion(UUID profileId, UUID questionId, LocalDateTime deletedAt) {
        this.profileId = profileId;
        this.questionId = questionId;
        this.deletedAt = deletedAt;
    }
}
