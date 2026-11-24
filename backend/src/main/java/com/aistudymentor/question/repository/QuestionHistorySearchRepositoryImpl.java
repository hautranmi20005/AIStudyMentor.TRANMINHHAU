package com.aistudymentor.question.repository;

import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.question.entity.Question;
import com.aistudymentor.question.entity.QuestionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Repository
public class QuestionHistorySearchRepositoryImpl implements QuestionHistorySearchRepository {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "createdAt", "created_at",
            "updatedAt", "updated_at",
            "subject", "subject",
            "status", "status");

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<Question> searchByProfile(
            StudentProfile profile,
            String query,
            String subject,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Boolean bookmarked,
            QuestionStatus status,
            Pageable pageable) {
        String database = databaseName();
        StringBuilder where = new StringBuilder("q.profile_id = :profileId");
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("profileId", profile.getProfileId().toString());
        if (query != null) {
            where.append(" AND ").append(searchPredicate(database));
            parameters.put("query", database.contains("h2") ? "%" + query.toLowerCase() + "%" : query);
        }
        if (subject != null) {
            where.append(" AND LOWER(q.subject) = LOWER(:subject)");
            parameters.put("subject", subject);
        }
        if (fromDate != null) {
            where.append(" AND q.created_at >= :fromDate");
            parameters.put("fromDate", fromDate);
        }
        if (toDate != null) {
            where.append(" AND q.created_at < :toDate");
            parameters.put("toDate", toDate);
        }
        if (status != null) {
            where.append(" AND q.status = :status");
            parameters.put("status", status.name());
        }
        if (bookmarked != null) {
            String exists = "EXISTS (SELECT 1 FROM question_bookmarks b "
                    + "WHERE b.profile_id = :profileId AND b.question_id = q.question_id)";
            where.append(bookmarked ? " AND " + exists : " AND NOT " + exists);
        }

        String orderBy = orderBy(pageable);
        Query resultQuery = entityManager.createNativeQuery(
                "SELECT q.* FROM questions q WHERE " + where + orderBy, Question.class);
        Query countQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM questions q WHERE " + where);
        parameters.forEach((name, value) -> {
            resultQuery.setParameter(name, value);
            countQuery.setParameter(name, value);
        });
        List<Question> content = castQuestions(resultQuery
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList());
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new PageImpl<>(content, pageable, total);
    }

    private String searchPredicate(String database) {
        if (database.contains("mysql")) {
            return "MATCH(q.subject, q.question_text) AGAINST (:query IN NATURAL LANGUAGE MODE)";
        }
        if (database.contains("postgresql")) {
            return "to_tsvector('simple', coalesce(q.subject, '') || ' ' || coalesce(q.question_text, '')) "
                    + "@@ plainto_tsquery('simple', :query)";
        }
        return "LOWER(CONCAT(COALESCE(q.subject, ''), ' ', COALESCE(q.question_text, ''))) LIKE :query";
    }

    private String orderBy(Pageable pageable) {
        Sort.Order order = pageable.getSort().stream().findFirst().orElse(Sort.Order.desc("createdAt"));
        String column = SORT_COLUMNS.get(order.getProperty());
        if (column == null) {
            throw new IllegalArgumentException("Unsupported question history sort property.");
        }
        return " ORDER BY q." + column + " " + order.getDirection().name() + ", q.question_id ASC";
    }

    private String databaseName() {
        try {
            return entityManager.unwrap(Session.class)
                    .doReturningWork(connection -> connection.getMetaData().getDatabaseProductName())
                    .toLowerCase(Locale.ROOT);
        } catch (org.hibernate.HibernateException exception) {
            throw new IllegalStateException("Could not determine the question search database dialect.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Question> castQuestions(List<?> rows) {
        List<Question> questions = new ArrayList<>(rows.size());
        for (Object row : rows) {
            questions.add((Question) row);
        }
        return questions;
    }
}
