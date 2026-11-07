package com.aistudymentor.question.entity;

import com.aistudymentor.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "answer_id", updatable = false, nullable = false)
    private UUID answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Lob
    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Lob
    @Column(name = "structured_json", columnDefinition = "TEXT", nullable = false)
    private String structuredJson;

    @Column(name = "is_alternative", nullable = false)
    @Builder.Default
    private Boolean isAlternative = false;
}
