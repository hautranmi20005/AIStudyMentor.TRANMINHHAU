package com.aistudymentor.question.repository;

import com.aistudymentor.question.entity.QuestionSyncDeletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionSyncDeletionRepository extends JpaRepository<QuestionSyncDeletion, UUID> {
    List<QuestionSyncDeletion> findByProfileIdAndDeletedAtAfterAndDeletedAtLessThanEqual(
            UUID profileId, LocalDateTime since, LocalDateTime until);
}
