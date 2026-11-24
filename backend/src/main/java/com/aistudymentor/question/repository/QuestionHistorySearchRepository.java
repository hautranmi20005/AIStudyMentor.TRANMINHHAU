package com.aistudymentor.question.repository;

import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.question.entity.Question;
import com.aistudymentor.question.entity.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface QuestionHistorySearchRepository {
    Page<Question> searchByProfile(
            StudentProfile profile,
            String query,
            String subject,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Boolean bookmarked,
            QuestionStatus status,
            Pageable pageable);
}
