package com.aistudymentor.question.repository;

import com.aistudymentor.question.entity.Answer;
import com.aistudymentor.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {
    List<Answer> findByQuestion(Question question);
    Optional<Answer> findFirstByQuestionAndIsAlternativeFalseOrderByCreatedAtDesc(Question question);
    Optional<Answer> findFirstByQuestionAndIsAlternativeTrueOrderByCreatedAtDesc(Question question);
}
