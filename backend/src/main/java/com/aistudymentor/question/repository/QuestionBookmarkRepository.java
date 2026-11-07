package com.aistudymentor.question.repository;

import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.question.entity.Question;
import com.aistudymentor.question.entity.QuestionBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionBookmarkRepository extends JpaRepository<QuestionBookmark, UUID> {
    Optional<QuestionBookmark> findByProfileAndQuestion(StudentProfile profile, Question question);
    boolean existsByProfileAndQuestion(StudentProfile profile, Question question);
    void deleteByProfileAndQuestion(StudentProfile profile, Question question);
    Page<QuestionBookmark> findByProfileOrderByCreatedAtDesc(StudentProfile profile, Pageable pageable);
}
