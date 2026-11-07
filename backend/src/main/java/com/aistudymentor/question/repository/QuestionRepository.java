package com.aistudymentor.question.repository;

import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.question.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    Page<Question> findByProfileOrderByCreatedAtDesc(StudentProfile profile, Pageable pageable);
    Optional<Question> findByQuestionIdAndProfile(UUID questionId, StudentProfile profile);
}
