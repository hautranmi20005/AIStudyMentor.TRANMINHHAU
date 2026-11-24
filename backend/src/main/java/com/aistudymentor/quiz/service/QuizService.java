package com.aistudymentor.quiz.service;

import com.aistudymentor.ai.dto.GeneratedQuiz;
import com.aistudymentor.ai.dto.GeneratedQuizQuestion;
import com.aistudymentor.ai.dto.QuizRequest;
import com.aistudymentor.ai.service.AiOrchestrator;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.aistudymentor.quiz.dto.QuizAttemptStartResponse;
import com.aistudymentor.quiz.dto.QuizAttemptHistoryDto;
import com.aistudymentor.quiz.dto.QuizCreateRequest;
import com.aistudymentor.quiz.dto.QuizEditResponseDto;
import com.aistudymentor.quiz.dto.QuizOptionDto;
import com.aistudymentor.quiz.dto.QuizQuestionEditDto;
import com.aistudymentor.quiz.dto.QuizQuestionDto;
import com.aistudymentor.quiz.dto.QuizResponseDto;
import com.aistudymentor.quiz.dto.QuizResponseItemDto;
import com.aistudymentor.quiz.dto.QuizResultItemDto;
import com.aistudymentor.quiz.dto.QuizSubmitRequest;
import com.aistudymentor.quiz.dto.QuizSubmitResponse;
import com.aistudymentor.quiz.dto.QuizUpdateRequest;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private static final Set<String> FALLBACK_AI_ERROR_CODES = Set.of(
            "AI_NOT_CONFIGURED",
            "AI_PROVIDER_ERROR",
            "AI_PROVIDER_TIMEOUT",
            "AI_RATE_LIMITED",
            "AI_INVALID_RESPONSE");
    private static final List<FallbackQuestion> STATIC_FALLBACK_QUESTIONS = List.of(
            new FallbackQuestion(
                    "When studying %s, what best supports a conclusion?",
                    List.of("Relevant evidence that directly supports the claim.",
                            "A confident guess with no supporting information.",
                            "An unrelated fact from a different subject."),
                    "Relevant evidence that directly supports the claim.",
                    "A conclusion is stronger when it follows from evidence relevant to the question."),
            new FallbackQuestion(
                    "Before relying on a source about %s, what should you check?",
                    List.of("Its author, evidence, and method.",
                            "Whether its title agrees with your first impression.",
                            "Whether it avoids describing how its claims were reached."),
                    "Its author, evidence, and method.",
                    "Checking authorship and evidence helps assess a source's reliability."),
            new FallbackQuestion(
                    "Which is the best way to summarize material about %s?",
                    List.of("Restate the central ideas accurately and briefly.",
                            "Copy every sentence without identifying the main point.",
                            "Add claims that were not present in the material."),
                    "Restate the central ideas accurately and briefly.",
                    "A summary focuses on the central ideas without adding unsupported claims."),
            new FallbackQuestion(
                    "Two explanations about %s disagree. What is the best next step?",
                    List.of("Compare the evidence and reasoning used by each explanation.",
                            "Choose the longer explanation without checking its support.",
                            "Ignore evidence that does not match your preferred answer."),
                    "Compare the evidence and reasoning used by each explanation.",
                    "Comparing support and reasoning is more reliable than choosing by length or preference."),
            new FallbackQuestion(
                    "What is an inference when studying %s?",
                    List.of("A conclusion drawn from evidence and prior knowledge.",
                            "A fact that has no connection to the available evidence.",
                            "A question copied without attempting to answer it."),
                    "A conclusion drawn from evidence and prior knowledge.",
                    "An inference interprets evidence; it is not an unsupported guess."),
            new FallbackQuestion(
                    "How should you check a surprising answer about %s?",
                    List.of("Review the reasoning and compare it with the original question.",
                            "Accept it immediately because it sounds complicated.",
                            "Change the question until the answer seems correct."),
                    "Review the reasoning and compare it with the original question.",
                    "Checking each step against the prompt can reveal mistakes or missing assumptions."),
            new FallbackQuestion(
                    "Why should you record the source of information about %s?",
                    List.of("So another reader can locate and verify the information.",
                            "So the evidence cannot be checked by anyone else.",
                            "So the source can be replaced by an unsupported claim."),
                    "So another reader can locate and verify the information.",
                    "Clear source details support verification and give credit to the original work."),
            new FallbackQuestion(
                    "When learning a new idea about %s, which approach best tests your understanding?",
                    List.of("Explain the idea in your own words and apply it to an example.",
                            "Memorize a phrase without checking what it means.",
                            "Avoid examples that could expose a misunderstanding."),
                    "Explain the idea in your own words and apply it to an example.",
                    "Explaining and applying an idea helps reveal whether you understand it."),
            new FallbackQuestion(
                    "A piece of evidence challenges a broad claim about %s. What should you do?",
                    List.of("Reconsider whether the claim needs evidence or a narrower scope.",
                            "Discard the evidence only because it is inconvenient.",
                            "Keep the claim unchanged without examining the conflict."),
                    "Reconsider whether the claim needs evidence or a narrower scope.",
                    "Conflicting evidence is a reason to test and refine a claim."),
            new FallbackQuestion(
                    "When comparing interpretations of %s, what matters most?",
                    List.of("How well each interpretation is supported by the relevant material.",
                            "Which interpretation uses the most difficult vocabulary.",
                            "Which interpretation was heard most recently."),
                    "How well each interpretation is supported by the relevant material.",
                    "Interpretations should be evaluated by their support, not by style or recency."));

    private final PracticeQuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizResponseRepository responseRepository;
    private final StudentProfileRepository profileRepository;
    private final AiOrchestrator aiOrchestrator;

    // ─────────────────────────── GENERATE QUIZ ───────────────────────────

    @Transactional
    public QuizResponseDto generateQuiz(UUID userId, QuizCreateRequest request) {
        StudentProfile profile = resolveProfile(userId);
        if (!StringUtils.hasText(profile.getEducationLevel())) {
            throw new BusinessException(HttpStatus.CONFLICT, "ONBOARDING_REQUIRED",
                    "Complete your learning profile before generating a quiz.");
        }

        PracticeQuiz quiz = PracticeQuiz.builder()
                .profile(profile)
                .subject(request.getSubject())
                .topic(StringUtils.hasText(request.getTopic()) ? request.getTopic().trim() : null)
                .status(QuizStatus.GENERATED)
                .build();
        quiz = quizRepository.save(quiz);

        int num = request.getNumQuestions() != null ? request.getNumQuestions() : 5;
        QuizRequest aiRequest = QuizRequest.builder()
                .subject(request.getSubject())
                .topic(request.getTopic())
                .educationLevel(profile.getEducationLevel())
                .numQuestions(num)
                .build();
        GeneratedQuiz generated = generateQuizWithFallback(profile, aiRequest, quiz.getQuizId(), request, num);
        List<QuizQuestion> questions = new ArrayList<>();

        for (GeneratedQuizQuestion generatedQuestion : generated.getQuestions()) {
            QuizQuestionType type = questionType(generatedQuestion.getType());
            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .type(type)
                    .prompt(generatedQuestion.getPrompt())
                    .correctAnswer(generatedQuestion.getCorrectAnswer())
                    .explanation(generatedQuestion.getExplanation())
                    .build();
            question = questionRepository.save(question);

            List<QuizOption> options = new ArrayList<>();
            if (generatedQuestion.getOptions() != null) {
                for (int index = 0; index < generatedQuestion.getOptions().size(); index++) {
                    options.add(createOption(question, generatedQuestion.getOptions().get(index), index + 1));
                }
            }
            question.setOptions(options);
            questions.add(question);
        }

        quiz.setQuestions(questions);
        log.info("Quiz generated: quizId={}, userId={}, numQuestions={}", quiz.getQuizId(), userId, num);
        return toQuizResponseDto(quiz);
    }

    private GeneratedQuiz generateQuizWithFallback(
            StudentProfile profile,
            QuizRequest aiRequest,
            UUID quizId,
            QuizCreateRequest request,
            int numQuestions) {
        try {
            GeneratedQuiz generated = aiOrchestrator.generateQuiz(profile, aiRequest, quizId).value();
            validateGeneratedQuiz(generated, numQuestions);
            return generated;
        } catch (BusinessException exception) {
            if (!FALLBACK_AI_ERROR_CODES.contains(exception.getCode())) {
                throw exception;
            }
            log.warn("AI quiz generation failed; using static fallback (quizId={}, code={}, status={})",
                    quizId, exception.getCode(), exception.getStatus().value());
        } catch (RuntimeException exception) {
            log.error("Unexpected AI quiz generation failure; using static fallback (quizId={}, exceptionType={})",
                    quizId, exception.getClass().getName());
        }
        return staticFallbackQuiz(request, numQuestions);
    }

    private GeneratedQuiz staticFallbackQuiz(QuizCreateRequest request, int numQuestions) {
        String context = StringUtils.hasText(request.getTopic())
                ? request.getTopic().trim()
                : request.getSubject().trim();
        List<GeneratedQuizQuestion> questions = STATIC_FALLBACK_QUESTIONS.stream()
                .limit(numQuestions)
                .map(template -> GeneratedQuizQuestion.builder()
                        .type(QuizQuestionType.MCQ.name())
                        .topic(context)
                        .prompt(template.prompt().formatted(context))
                        .options(template.options())
                        .correctAnswer(template.correctAnswer())
                        .acceptedAnswers(List.of())
                        .explanation(template.explanation())
                        .build())
                .toList();
        GeneratedQuiz fallback = GeneratedQuiz.builder()
                .questions(questions)
                .modelId("static-fallback")
                .promptVersion("quiz/fallback/v1")
                .inputTokens(0)
                .outputTokens(0)
                .build();
        validateGeneratedQuiz(fallback, numQuestions);
        return fallback;
    }

    private record FallbackQuestion(
            String prompt,
            List<String> options,
            String correctAnswer,
            String explanation) {
    }

    @Transactional
    public QuizResponseDto updateQuiz(UUID userId, UUID quizId, QuizUpdateRequest request) {
        PracticeQuiz quiz = findOwnedQuiz(userId, quizId);
        requireNotStarted(quiz);
        quiz.setSubject(request.getSubject().trim());
        if (request.getTopic() != null) {
            quiz.setTopic(StringUtils.hasText(request.getTopic()) ? request.getTopic().trim() : null);
        }
        if (request.getQuestions() != null) {
            validateQuestionEdits(request.getQuestions());
            quiz.getQuestions().clear();
            for (QuizQuestionEditDto edit : request.getQuestions()) {
                String correctAnswer = edit.getOptions().stream()
                        .filter(option -> QuizAnswerNormalizer.normalize(option)
                                .equals(QuizAnswerNormalizer.normalize(edit.getCorrectAnswer())))
                        .findFirst()
                        .orElseThrow(this::invalidQuizEdit);
                QuizQuestion question = questionRepository.save(QuizQuestion.builder()
                        .quiz(quiz)
                        .type(QuizQuestionType.MCQ)
                        .prompt(edit.getPrompt().trim())
                        .correctAnswer(correctAnswer)
                        .explanation(edit.getExplanation().trim())
                        .build());
                List<QuizOption> options = new ArrayList<>();
                for (int index = 0; index < edit.getOptions().size(); index++) {
                    options.add(createOption(question, edit.getOptions().get(index).trim(), index + 1));
                }
                question.setOptions(options);
                quiz.getQuestions().add(question);
            }
        }
        return toQuizResponseDto(quizRepository.save(quiz));
    }

    @Transactional
    public void deleteQuiz(UUID userId, UUID quizId) {
        PracticeQuiz quiz = findOwnedQuiz(userId, quizId);
        requireNotStarted(quiz);
        quizRepository.delete(quiz);
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
                                    .explanation(response.getQuestion().getExplanation())
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

    @Transactional(readOnly = true)
    public QuizEditResponseDto getQuizForEdit(UUID userId, UUID quizId) {
        PracticeQuiz quiz = findOwnedQuiz(userId, quizId);
        requireNotStarted(quiz);
        List<QuizQuestionEditDto> questions = quiz.getQuestions().stream()
                .map(question -> QuizQuestionEditDto.builder()
                        .prompt(question.getPrompt())
                        .options(question.getOptions().stream()
                                .sorted(java.util.Comparator.comparing(QuizOption::getOptionOrder))
                                .map(QuizOption::getOptionText)
                                .toList())
                        .correctAnswer(question.getCorrectAnswer())
                        .explanation(question.getExplanation() == null ? "" : question.getExplanation())
                        .build())
                .toList();
        return QuizEditResponseDto.builder()
                .quizId(quiz.getQuizId())
                .subject(quiz.getSubject())
                .topic(quiz.getTopic())
                .questions(questions)
                .build();
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
        if (request == null || request.getResponses() == null) {
            throw invalidQuizResponse();
        }
        StudentProfile profile = resolveProfile(userId);
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ATTEMPT_NOT_FOUND",
                        "Quiz attempt not found."));

        if (!attempt.getProfile().getProfileId().equals(profile.getProfileId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Attempt does not belong to you.");
        }

        PracticeQuiz quiz = attempt.getQuiz();
        if (!quiz.getQuizId().equals(quizId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ATTEMPT_NOT_FOUND", "Quiz attempt not found.");
        }
        if (attempt.getEndTime() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "ATTEMPT_ALREADY_SUBMITTED",
                    "This quiz attempt has already been submitted.");
        }
        Map<UUID, QuizQuestion> questionMap = quiz.getQuestions().stream()
                .collect(Collectors.toMap(QuizQuestion::getQuestionId, Function.identity()));

        Map<UUID, QuizResponseItemDto> submittedResponses = new java.util.HashMap<>();
        for (QuizResponseItemDto response : request.getResponses()) {
            if (response == null || !questionMap.containsKey(response.getQuestionId())
                    || submittedResponses.putIfAbsent(response.getQuestionId(), response) != null) {
                throw invalidQuizResponse();
            }
        }

        int correctCount = 0;
        List<QuizResultItemDto> results = new ArrayList<>();

        for (QuizQuestion question : quiz.getQuestions()) {
            QuizResponseItemDto respDto = submittedResponses.get(question.getQuestionId());

            QuizOption selectedOption = null;
            if (respDto != null && respDto.getSelectedOptionId() != null) {
                selectedOption = optionRepository.findById(respDto.getSelectedOptionId()).orElse(null);
                if (selectedOption == null || selectedOption.getQuestion() == null
                        || !question.getQuestionId().equals(selectedOption.getQuestion().getQuestionId())) {
                    throw invalidQuizResponse();
                }
            }

            boolean isCorrect = false;
            if (question.getType() == QuizQuestionType.MCQ) {
                if (respDto != null && respDto.getSubjectiveText() != null) {
                    throw invalidQuizResponse();
                }
                isCorrect = selectedOption != null
                        && QuizAnswerNormalizer.normalize(selectedOption.getOptionText())
                        .equals(QuizAnswerNormalizer.normalize(question.getCorrectAnswer()));
            } else {
                if (selectedOption != null) {
                    throw invalidQuizResponse();
                }
                isCorrect = respDto != null && StringUtils.hasText(respDto.getSubjectiveText())
                        && QuizAnswerNormalizer.normalize(respDto.getSubjectiveText())
                        .equals(QuizAnswerNormalizer.normalize(question.getCorrectAnswer()));
            }

            if (isCorrect) {
                correctCount++;
            }

            QuizResponse quizResponse = QuizResponse.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOptionId(selectedOption != null ? selectedOption.getOptionId() : null)
                    .subjectiveText(respDto == null ? null : respDto.getSubjectiveText())
                    .isCorrect(isCorrect)
                    .build();
            responseRepository.save(quizResponse);

            results.add(QuizResultItemDto.builder()
                    .questionId(question.getQuestionId())
                    .isCorrect(isCorrect)
                    .correctAnswer(question.getCorrectAnswer())
                    .explanation(question.getExplanation())
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

    private BusinessException invalidQuizResponse() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_QUIZ_RESPONSE",
                "Quiz responses must reference each question and its options in this quiz.");
    }

    private PracticeQuiz findOwnedQuiz(UUID userId, UUID quizId) {
        StudentProfile profile = resolveProfile(userId);
        return quizRepository.findByQuizIdAndProfile(quizId, profile)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Quiz not found or does not belong to you."));
    }

    private void requireNotStarted(PracticeQuiz quiz) {
        if (quiz.getStatus() != QuizStatus.GENERATED || !quiz.getAttempts().isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "QUIZ_ALREADY_STARTED",
                    "A quiz cannot be changed after an attempt has started.");
        }
    }

    private QuizQuestionType questionType(String value) {
        if (!StringUtils.hasText(value)) {
            throw invalidGeneratedQuiz();
        }
        try {
            return QuizQuestionType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalidGeneratedQuiz();
        }
    }

    private void validateGeneratedQuiz(GeneratedQuiz generated, int expectedCount) {
        if (generated == null || generated.getQuestions() == null
                || generated.getQuestions().size() != expectedCount) {
            throw invalidGeneratedQuiz();
        }
        for (GeneratedQuizQuestion question : generated.getQuestions()) {
            if (question == null || !StringUtils.hasText(question.getPrompt())
                    || question.getPrompt().length() > 2000
                    || !StringUtils.hasText(question.getCorrectAnswer())
                    || !StringUtils.hasText(question.getExplanation())
                    || question.getExplanation().length() > 2000) {
                throw invalidGeneratedQuiz();
            }
            QuizQuestionType type = questionType(question.getType());
            List<String> options = question.getOptions();
            if (type != QuizQuestionType.MCQ || options == null || options.size() < 3 || options.size() > 5
                    || options.stream().anyMatch(option -> !StringUtils.hasText(option) || option.length() > 255)
                    || options.stream().map(QuizAnswerNormalizer::normalize).distinct().count() != options.size()
                    || options.stream().filter(option -> QuizAnswerNormalizer.normalize(option)
                    .equals(QuizAnswerNormalizer.normalize(question.getCorrectAnswer()))).count() != 1) {
                throw invalidGeneratedQuiz();
            }
        }
    }

    private void validateQuestionEdits(List<QuizQuestionEditDto> questions) {
        if (questions.isEmpty() || questions.size() > 10) {
            throw invalidQuizEdit();
        }
        for (QuizQuestionEditDto question : questions) {
            if (question == null || !StringUtils.hasText(question.getPrompt())
                    || question.getPrompt().length() > 2000
                    || !StringUtils.hasText(question.getExplanation())
                    || question.getExplanation().length() > 2000
                    || !StringUtils.hasText(question.getCorrectAnswer())
                    || question.getCorrectAnswer().length() > 255
                    || question.getOptions() == null
                    || question.getOptions().size() < 3 || question.getOptions().size() > 5
                    || question.getOptions().stream().anyMatch(option -> !StringUtils.hasText(option)
                    || option.length() > 255)
                    || question.getOptions().stream().map(QuizAnswerNormalizer::normalize).distinct().count()
                    != question.getOptions().size()
                    || question.getOptions().stream().filter(option -> QuizAnswerNormalizer.normalize(option)
                    .equals(QuizAnswerNormalizer.normalize(question.getCorrectAnswer()))).count() != 1) {
                throw invalidQuizEdit();
            }
        }
    }

    private BusinessException invalidQuizEdit() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_QUIZ_EDIT",
                "Each quiz question must have a prompt, explanation, 3 to 5 distinct options, and one correct option.");
    }

    private BusinessException invalidGeneratedQuiz() {
        return new BusinessException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE",
                "The AI provider returned an invalid quiz. Please retry the request.");
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
                .topic(quiz.getTopic())
                .status(quiz.getStatus())
                .questions(questionDtos)
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}
