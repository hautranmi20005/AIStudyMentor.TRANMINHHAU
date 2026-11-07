package com.aistudymentor.quiz.repository;

import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.quiz.entity.PracticeQuiz;
import com.aistudymentor.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    List<QuizAttempt> findByProfileOrderByCreatedAtDesc(StudentProfile profile);
    Optional<QuizAttempt> findByAttemptIdAndProfile(UUID attemptId, StudentProfile profile);
    List<QuizAttempt> findByQuizAndProfile(PracticeQuiz quiz, StudentProfile profile);
}
