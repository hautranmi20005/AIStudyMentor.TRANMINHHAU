package com.aistudymentor.ai.repository;

import com.aistudymentor.ai.entity.AiGenerationLog;
import com.aistudymentor.profile.entity.StudentProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {
    Page<AiGenerationLog> findByProfileOrderByCreatedAtDesc(StudentProfile profile, Pageable pageable);
}
