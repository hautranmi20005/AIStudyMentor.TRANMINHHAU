package com.aistudymentor.quiz.service;

import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.aistudymentor.quiz.dto.QuizAttemptStartResponse;
import com.aistudymentor.quiz.dto.QuizAttemptHistoryDto;
import com.aistudymentor.quiz.dto.QuizCreateRequest;
import com.aistudymentor.quiz.dto.QuizOptionDto;
import com.aistudymentor.quiz.dto.QuizQuestionDto;
import com.aistudymentor.quiz.dto.QuizResponseDto;
import com.aistudymentor.quiz.dto.QuizResponseItemDto;
import com.aistudymentor.quiz.dto.QuizResultItemDto;
import com.aistudymentor.quiz.dto.QuizSubmitRequest;
import com.aistudymentor.quiz.dto.QuizSubmitResponse;
import com.aistudymentor.quiz.entity.PracticeQuiz;
import com.aistudymentor.quiz.entity.QuizAttempt;
import com.aistudymentor.quiz.entity.QuizOption;
import com.aistudymentor.quiz.entity.QuizQuestion;
import com.aistudymentor.quiz.entity.QuizQuestionType;
import com.aistudymentor.quiz.entity.QuizResponse;
import com.aistudymentor.quiz.entity.QuizStatus;
import com.aistudymentor.quiz.repository.PracticeQuizRepository;
import com.aistudymentor.quiz.repository.QuizAttemptRepository;
import com.aistudymentor.quiz.repository.QuizOptionRepository;
import com.aistudymentor.quiz.repository.QuizQuestionRepository;
import com.aistudymentor.quiz.repository.QuizResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final PracticeQuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizResponseRepository responseRepository;
    private final StudentProfileRepository profileRepository;

    // ─────────────────────────── GENERATE QUIZ ───────────────────────────

    @Transactional
    public QuizResponseDto generateQuiz(UUID userId, QuizCreateRequest request) {
        StudentProfile profile = resolveProfile(userId);

        PracticeQuiz quiz = PracticeQuiz.builder()
                .profile(profile)
                .subject(request.getSubject())
                .status(QuizStatus.GENERATED)
                .build();
        quiz = quizRepository.save(quiz);

        int num = request.getNumQuestions() != null ? request.getNumQuestions() : 5;
        List<QuizQuestion> questions = new ArrayList<>();

        for (int i = 1; i <= num; i++) {
            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .type(QuizQuestionType.MCQ)
                    .prompt("Practice Question #" + i + " for " + request.getSubject())
                    .correctAnswer("Option B")
                    .build();
            question = questionRepository.save(question);

            List<QuizOption> options = List.of(
                    createOption(question, "Option A", 1),
                    createOption(question, "Option B", 2),
                    createOption(question, "Option C", 3),
                    createOption(question, "Option D", 4)
            );
            question.setOptions(options);
            questions.add(question);
        }

        quiz.setQuestions(questions);
        log.info("Quiz generated: quizId={}, userId={}, numQuestions={}", quiz.getQuizId(), userId, num);
        return toQuizResponseDto(quiz);
    }

    // ─────────────────────────── LIST QUIZZES ───────────────────────────

    @Transactional(readOnly = true)
    public Page<QuizResponseDto> listQuizzes(UUID userId, Pageable pageable) {
        StudentProfile profile = resolveProfile(userId);
        return quizRepository.findByProfileOrderByCreatedAtDesc(profile, pageable)
                .map(this::toQuizResponseDto);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptHistoryDto> listCompletedAttempts(UUID userId) {
        StudentProfile profile = resolveProfile(userId);
        return attemptRepository.findByProfileAndEndTimeIsNotNullOrderByEndTimeDesc(profile).stream()
                .map(attempt -> {
                    List<QuizResultItemDto> results = attempt.getResponses().stream()
                            .map(response -> QuizResultItemDto.builder()
                                    .questionId(response.getQuestion().getQuestionId())
                                    .isCorrect(response.getIsCorrect())
                                    .correctAnswer(response.getQuestion().getCorrectAnswer())
                                    .explanation("Correct answer: " + response.getQuestion().getCorrectAnswer())
                                    .build())
                            .toList();
                    int totalQuestions = attempt.getQuiz().getQuestions().size();
                    int correctCount = (int) results.stream()
                            .filter(result -> Boolean.TRUE.equals(result.getIsCorrect()))
                            .count();
                    return QuizAttemptHistoryDto.builder()
                            .attemptId(attempt.getAttemptId())
                            .quizId(attempt.getQuiz().getQuizId())
                            .subject(attempt.getQuiz().getSubject())
                            .score(attempt.getScore())
                            .totalQuestions(totalQuestions)
                            .correctCount(correctCount)
                            .submittedAt(attempt.getEndTime())
                            .results(results)
                            .build();
                })
                .toList();
    }

    // ─────────────────────────── GET QUIZ DETAIL ───────────────────────────

    @Transactional(readOnly = true)
    public QuizResponseDto getQuiz(UUID userId, UUID quizId) {
        StudentProfile profile = resolveProfile(userId);
        PracticeQuiz quiz = quizRepository.findByQuizIdAndProfile(quizId, profile)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Quiz not found or does not belong to you."));

        return toQuizResponseDto(quiz);
    }

    // ─────────────────────────── START ATTEMPT ───────────────────────────

    @Transactional
    public QuizAttemptStartResponse startAttempt(UUID userId, UUID quizId) {
        StudentProfile profile = resolveProfile(userId);
        PracticeQuiz quiz = quizRepository.findByQuizIdAndProfile(quizId, profile)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Quiz not found or does not belong to you."));

        quiz.setStatus(QuizStatus.IN_PROGRESS);
        quizRepository.save(quiz);

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .profile(profile)
                .startTime(LocalDateTime.now())
                .score(0)
                .build();
        attempt = attemptRepository.save(attempt);

        log.info("Quiz attempt started: attemptId={}, quizId={}, userId={}", attempt.getAttemptId(), quizId, userId);
        return QuizAttemptStartResponse.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(quizId)
                .startTime(attempt.getStartTime())
                .build();
    }

    // ─────────────────────────── SUBMIT ATTEMPT & GRADE ───────────────────────────

    @Transactional
    public QuizSubmitResponse submitAttempt(UUID userId, UUID quizId, UUID attemptId, QuizSubmitRequest request) {
        StudentProfile profile = resolveProfile(userId);
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ATTEMPT_NOT_FOUND",
                        "Quiz attempt not found."));

        if (!attempt.getProfile().getProfileId().equals(profile.getProfileId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Attempt does not belong to you.");
        }

        PracticeQuiz quiz = attempt.getQuiz();
        Map<UUID, QuizQuestion> questionMap = quiz.getQuestions().stream()
                .collect(Collectors.toMap(QuizQuestion::getQuestionId, Function.identity()));

        int correctCount = 0;
        List<QuizResultItemDto> results = new ArrayList<>();

        for (QuizResponseItemDto respDto : request.getResponses()) {
            QuizQuestion question = questionMap.get(respDto.getQuestionId());
            if (question == null) {
                continue;
            }

            QuizOption selectedOption = null;
            if (respDto.getSelectedOptionId() != null) {
                selectedOption = optionRepository.findById(respDto.getSelectedOptionId()).orElse(null);
            }

            boolean isCorrect = false;
            if (selectedOption != null) {
                // MCQ grading
                isCorrect = selectedOption.getOptionText().equalsIgnoreCase(question.getCorrectAnswer());
            } else if (respDto.getSubjectiveText() != null) {
                // Short answer grading (normalized whitespace & casing)
                isCorrect = respDto.getSubjectiveText().trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
            }

            if (isCorrect) {
                correctCount++;
            }

            QuizResponse quizResponse = QuizResponse.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOptionId(selectedOption != null ? selectedOption.getOptionId() : null)
                    .subjectiveText(respDto.getSubjectiveText())
                    .isCorrect(isCorrect)
                    .build();
            responseRepository.save(quizResponse);

            results.add(QuizResultItemDto.builder()
                    .questionId(question.getQuestionId())
                    .isCorrect(isCorrect)
                    .correctAnswer(question.getCorrectAnswer())
                    .explanation("Correct answer: " + question.getCorrectAnswer())
                    .build());
        }

        int totalQuestions = quiz.getQuestions().size();
        int score = totalQuestions > 0 ? (correctCount * 100) / totalQuestions : 0;

        attempt.setScore(score);
        attempt.setEndTime(LocalDateTime.now());
        attemptRepository.save(attempt);

        quiz.setStatus(QuizStatus.COMPLETED);
        quizRepository.save(quiz);

        log.info("Quiz attempt submitted: attemptId={}, score={}, correct={}/{}", attemptId, score, correctCount, totalQuestions);

        return QuizSubmitResponse.builder()
                .attemptId(attemptId)
                .score(score)
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .results(results)
                .submittedAt(attempt.getEndTime())
                .build();
    }

    // ─────────────────────────── HELPER METHODS ───────────────────────────

    private QuizOption createOption(QuizQuestion question, String text, int order) {
        QuizOption option = QuizOption.builder()
                .question(question)
                .optionText(text)
                .optionOrder(order)
                .build();
        return optionRepository.save(option);
    }

    private StudentProfile resolveProfile(UUID userId) {
        return profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                        "Student profile not found."));
    }

    private QuizResponseDto toQuizResponseDto(PracticeQuiz quiz) {
        List<QuizQuestionDto> questionDtos = quiz.getQuestions().stream()
                .map(q -> QuizQuestionDto.builder()
                        .questionId(q.getQuestionId())
                        .type(q.getType())
                        .prompt(q.getPrompt())
                        // Crucial security assertion: correct_answer is strictly omitted!
                        .options(q.getOptions().stream()
                                .map(o -> QuizOptionDto.builder()
                                        .optionId(o.getOptionId())
                                        .optionText(o.getOptionText())
                                        .optionOrder(o.getOptionOrder())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return QuizResponseDto.builder()
                .quizId(quiz.getQuizId())
                .subject(quiz.getSubject())
                .status(quiz.getStatus())
                .questions(questionDtos)
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}
