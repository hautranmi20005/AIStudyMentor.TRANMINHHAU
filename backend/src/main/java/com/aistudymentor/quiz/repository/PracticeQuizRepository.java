package com.aistudymentor.quiz.repository;

import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.quiz.entity.PracticeQuiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PracticeQuizRepository extends JpaRepository<PracticeQuiz, UUID> {
    Page<PracticeQuiz> findByProfileOrderByCreatedAtDesc(StudentProfile profile, Pageable pageable);
    Optional<PracticeQuiz> findByQuizIdAndProfile(UUID quizId, StudentProfile profile);
}
