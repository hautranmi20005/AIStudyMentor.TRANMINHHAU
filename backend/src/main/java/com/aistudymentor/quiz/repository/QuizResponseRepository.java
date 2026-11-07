package com.aistudymentor.quiz.repository;

import com.aistudymentor.quiz.entity.QuizAttempt;
import com.aistudymentor.quiz.entity.QuizResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizResponseRepository extends JpaRepository<QuizResponse, UUID> {
    List<QuizResponse> findByAttempt(QuizAttempt attempt);
}
