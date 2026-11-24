package com.aistudymentor.question.repository;

import com.aistudymentor.question.entity.AnswerReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnswerReportRepository extends JpaRepository<AnswerReport, UUID> {
}
