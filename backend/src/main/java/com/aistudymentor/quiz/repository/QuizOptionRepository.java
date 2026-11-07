package com.aistudymentor.quiz.repository;

import com.aistudymentor.quiz.entity.QuizOption;
import com.aistudymentor.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, UUID> {
    List<QuizOption> findByQuestionOrderByOptionOrderAsc(QuizQuestion question);
}
