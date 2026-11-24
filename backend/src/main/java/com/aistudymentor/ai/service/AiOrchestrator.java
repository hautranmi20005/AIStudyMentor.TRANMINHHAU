package com.aistudymentor.ai.service;

import com.aistudymentor.ai.dto.AnswerRequest;
import com.aistudymentor.ai.dto.GeneratedQuiz;
import com.aistudymentor.ai.dto.QuizRequest;
import com.aistudymentor.ai.dto.StructuredAnswer;
import com.aistudymentor.ai.entity.AiGenerationLog;
import com.aistudymentor.ai.metrics.AiMetrics;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.profile.entity.StudentProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class AiOrchestrator {

    private static final int MAX_QUESTION_LENGTH = 8000;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_SUBJECTS = Set.of(
            "mathematics", "physics", "chemistry", "biology", "science", "history", "literature",
            "programming", "languages", "auto-detect");
    private static final Set<String> EXPLANATION_STYLES = Set.of("short", "detailed", "step-by-step");

    private final AiProvider provider;
    private final AiResponseCache responseCache;
    private final AiMetrics metrics;
    private final AiSafetyClassifier safetyClassifier;
    private final PromptTemplateService prompts;
    private final AiQuotaService quotas;
    private final AiUsageLogger usageLogger;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Semaphore calls;
    private final ConcurrentMap<String, CompletableFuture<StructuredAnswer>> inFlight = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<GeneratedQuiz>> quizzesInFlight = new ConcurrentHashMap<>();

    public AiOrchestrator(
            AiProvider provider,
            AiResponseCache responseCache,
            AiMetrics metrics,
            AiSafetyClassifier safetyClassifier,
            PromptTemplateService prompts,
            AiQuotaService quotas,
            AiUsageLogger usageLogger,
            ObjectMapper objectMapper,
            Validator validator,
            @Value("${app.ai.max-concurrent-calls:8}") int maxConcurrentCalls) {
        this.provider = provider;
        this.responseCache = responseCache;
        this.metrics = metrics;
        this.safetyClassifier = safetyClassifier;
        this.prompts = prompts;
        this.quotas = quotas;
        this.usageLogger = usageLogger;
        this.objectMapper = objectMapper;
        this.validator = validator;
        if (maxConcurrentCalls < 1) {
            throw new IllegalArgumentException("app.ai.max-concurrent-calls must be positive.");
        }
        this.calls = new Semaphore(maxConcurrentCalls);
    }

    public AiExecution<StructuredAnswer> answer(
            StudentProfile profile, AnswerRequest request, java.util.UUID questionId) {
        validateRequest(request);
        if (safetyClassifier.classify(request.getQuestionText())
                == AiSafetyClassifier.Classification.OUT_OF_SCOPE) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUT_OF_SCOPE",
                    "I can help with school subjects and learning questions. Please ask an age-appropriate academic question.");
        }

        PromptTemplateService.Prompt prompt = prompts.answer(request);
        String taskType = request.isAlternative()
                ? "alternative:" + request.getAlternativeType()
                : "answer";
        String cacheKey = responseCache.keyForUser(profile.getProfileId(),
                request.getQuestionText(), request.getSubject(),
                request.getEducationLevel(), request.getExplanationStyle(), request.getAgeGroup(),
                request.getAnswerLanguage(), taskType, prompt.version(), provider.modelId(),
                request.getImageMimeType(), request.getImageBytes(), 0);
        StructuredAnswer cached = responseCache.get(cacheKey, StructuredAnswer.class);
        if (cached != null) {
            try {
                validateAndSanitize(cached, request.getSubject());
                validateOutputSafety(cached);
                metrics.recordAiCacheHit();
                return cachedExecution(profile, request, questionId, prompt, cached, true);
            } catch (BusinessException exception) {
                responseCache.evict(cacheKey);
                metrics.recordAiCacheError();
            }
        }

        CompletableFuture<StructuredAnswer> pending = new CompletableFuture<>();
        CompletableFuture<StructuredAnswer> active = inFlight.putIfAbsent(cacheKey, pending);
        if (active != null) {
            metrics.recordAiCacheHit();
            StructuredAnswer shared = awaitSharedAnswer(active);
            return cachedExecution(profile, request, questionId, prompt, copyAnswer(shared), true);
        }

        metrics.recordAiCacheMiss();
        long start = System.nanoTime();
        boolean acquired = false;
        AiQuotaService.Reservation reservation = null;
        boolean quotaReconciled = false;
        try {
            acquired = calls.tryAcquire(0, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw rateLimited("AI capacity is busy. Please retry shortly.");
            }
            reservation = quotas.reserve(profile.getProfileId());
            StructuredAnswer answer = provider.answer(request);
            validateAndSanitize(answer, request.getSubject());
            validateOutputSafety(answer);
            answer.setModelId(answer.getModelId() == null ? provider.modelId() : answer.getModelId());
            answer.setPromptVersion(prompt.version());
            answer.setCacheHit(false);
            responseCache.put(cacheKey, cacheValue(answer));
            long tokens = tokenCount(answer);
            quotas.reconcile(reservation, tokens);
            quotaReconciled = true;
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            long remainingCallsToday = quotas.remaining(profile.getProfileId()).getRemainingCallsToday();
            AiGenerationLog log = usageLogger.log(profile, questionId, null, answer.getModelId(),
                    prompt.version(), answer.getInputTokens(), answer.getOutputTokens(), latencyMs, false, "SUCCESS");
            answer.setPromptVersion(prompt.version());
            answer.setRemainingCallsToday(remainingCallsToday);
            pending.complete(copyAnswer(answer));
            return new AiExecution<>(answer, log, remainingCallsToday);
        } catch (InterruptedException exception) {
            if (reservation != null && !quotaReconciled) {
                quotas.reconcile(reservation, 0);
            }
            pending.completeExceptionally(exception);
            usageLogger.log(profile, questionId, null, provider.modelId(), prompt.version(),
                    null, null, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), false, "AI_UNAVAILABLE");
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                    "The AI service is temporarily unavailable. Please retry shortly.");
        } catch (RuntimeException exception) {
            if (reservation != null && !quotaReconciled) {
                quotas.reconcile(reservation, 0);
            }
            pending.completeExceptionally(exception);
            usageLogger.log(profile, questionId, null, provider.modelId(), prompt.version(),
                    null, null, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), false,
                    failureOutcome(exception));
            throw exception;
        } finally {
            inFlight.remove(cacheKey, pending);
            if (acquired) {
                calls.release();
            }
        }
    }

    public AiExecution<GeneratedQuiz> generateQuiz(StudentProfile profile, QuizRequest request, java.util.UUID quizId) {
        if (!StringUtils.hasText(request.getSubject()) || !allowedSubject(request.getSubject())
                || request.getNumQuestions() < 1 || request.getNumQuestions() > 10) {
            throw invalidRequest();
        }

        PromptTemplateService.Prompt prompt = prompts.quiz(request);
        String cacheKey = responseCache.keyForUser(profile.getProfileId(),
                quizContext(request), request.getSubject(), request.getEducationLevel(),
                request.getExplanationStyle(), request.getAgeGroup(), request.getAnswerLanguage(), "quiz",
                prompt.version(), provider.modelId(), null, null, request.getNumQuestions());
        GeneratedQuiz cached = responseCache.get(cacheKey, GeneratedQuiz.class);
        if (cached != null) {
            try {
                validateAndSanitizeQuiz(cached, request);
                validateQuizOutputSafety(cached);
                metrics.recordAiCacheHit();
                return cachedQuizExecution(profile, quizId, prompt, cached);
            } catch (BusinessException exception) {
                responseCache.evict(cacheKey);
                metrics.recordAiCacheError();
            }
        }

        CompletableFuture<GeneratedQuiz> pending = new CompletableFuture<>();
        CompletableFuture<GeneratedQuiz> active = quizzesInFlight.putIfAbsent(cacheKey, pending);
        if (active != null) {
            metrics.recordAiCacheHit();
            return cachedQuizExecution(profile, quizId, prompt, copyQuiz(awaitSharedQuiz(active)));
        }

        metrics.recordAiCacheMiss();
        long start = System.nanoTime();
        boolean acquired = false;
        AiQuotaService.Reservation reservation = null;
        boolean quotaReconciled = false;
        try {
            acquired = calls.tryAcquire(0, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw rateLimited("AI capacity is busy. Please retry shortly.");
            }
            reservation = quotas.reserve(profile.getProfileId());
            GeneratedQuiz result = provider.generateQuiz(request);
            validateAndSanitizeQuiz(result, request);
            validateQuizOutputSafety(result);
            result.setModelId(provider.modelId());
            result.setPromptVersion(prompt.version());
            responseCache.put(cacheKey, cacheValue(result));
            quotas.reconcile(reservation, tokenCount(result));
            quotaReconciled = true;
            long remainingCallsToday = quotas.remaining(profile.getProfileId()).getRemainingCallsToday();
            AiGenerationLog log = usageLogger.log(profile, null, quizId, result.getModelId(), prompt.version(),
                    result.getInputTokens(), result.getOutputTokens(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), false, "SUCCESS");
            pending.complete(copyQuiz(result));
            return new AiExecution<>(result, log, remainingCallsToday);
        } catch (InterruptedException exception) {
            if (reservation != null && !quotaReconciled) {
                quotas.reconcile(reservation, 0);
            }
            pending.completeExceptionally(exception);
            usageLogger.log(profile, null, quizId, provider.modelId(), prompt.version(), null, null,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), false, "AI_UNAVAILABLE");
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                    "The AI service is temporarily unavailable. Please retry shortly.");
        } catch (RuntimeException exception) {
            if (reservation != null && !quotaReconciled) {
                quotas.reconcile(reservation, 0);
            }
            pending.completeExceptionally(exception);
            usageLogger.log(profile, null, quizId, provider.modelId(), prompt.version(), null, null,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), false, failureOutcome(exception));
            throw exception;
        } finally {
            quizzesInFlight.remove(cacheKey, pending);
            if (acquired) {
                calls.release();
            }
        }
    }

    public com.aistudymentor.ai.dto.AiQuotaResponse quota(java.util.UUID profileId) {
        return quotas.remaining(profileId);
    }

    private void validateRequest(AnswerRequest request) {
        if (request == null || validator.validate(request).size() > 0
                || !allowedSubject(request.getSubject())
                || !EXPLANATION_STYLES.contains(request.getExplanationStyle())
                || (!StringUtils.hasText(request.getQuestionText()) && request.getImageBytes() == null)
                || request.getQuestionText() != null && request.getQuestionText().length() > MAX_QUESTION_LENGTH
                || request.getImageBytes() != null && (request.getImageBytes().length == 0
                || request.getImageBytes().length > MAX_IMAGE_BYTES
                || request.getImageMimeType() == null
                || !Set.of("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif")
                .contains(request.getImageMimeType().toLowerCase(Locale.ROOT)))) {
            throw invalidRequest();
        }
        AiSafetyClassifier.Classification classification = safetyClassifier.classify(request.getQuestionText());
        if (classification == AiSafetyClassifier.Classification.INVALID && request.getImageBytes() == null) {
            throw invalidRequest();
        }
    }

    private void validateAndSanitize(StructuredAnswer answer, String requestedSubject) {
        if (answer == null || !validator.validate(answer).isEmpty()
                || !requestedSubject.equalsIgnoreCase(answer.getSubject())) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE",
                    "The AI provider returned an invalid explanation. Please retry the request.");
        }
        answer.setSubject(requestedSubject);
        answer.setDifficulty(clean(answer.getDifficulty()));
        answer.setSummary(clean(answer.getSummary()));
        answer.setFinalAnswer(clean(answer.getFinalAnswer()));
        answer.setAlternativeExplanation(clean(answer.getAlternativeExplanation()));
        answer.setSteps(answer.getSteps().stream().map(step -> {
            step.setTitle(clean(step.getTitle()));
            step.setExplanation(clean(step.getExplanation()));
            step.setFormula(clean(step.getFormula()));
            return step;
        }).toList());
        answer.setKeyConcepts(cleanList(answer.getKeyConcepts()));
        answer.setCommonMistakes(cleanList(answer.getCommonMistakes()));
        answer.setFollowUpQuestions(cleanList(answer.getFollowUpQuestions()));
    }

    private void validateOutputSafety(StructuredAnswer answer) {
        String output = String.join("\n",
                answer.getSubject(),
                answer.getDifficulty(),
                answer.getSummary(),
                answer.getFinalAnswer(),
                answer.getAlternativeExplanation(),
                String.join("\n", answer.getKeyConcepts()),
                String.join("\n", answer.getCommonMistakes()),
                String.join("\n", answer.getFollowUpQuestions()),
                answer.getSteps().stream()
                        .map(step -> String.join("\n", step.getTitle(), step.getExplanation(), step.getFormula()))
                        .collect(java.util.stream.Collectors.joining("\n")));
        if (output.matches("(?is).*\\b(how to build a bomb|how to poison|instructions to harm someone)\\b.*")) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_BLOCKED_CONTENT",
                    "I can’t provide that explanation, but I can help with a safe academic topic.");
        }
    }

    private void sanitizeQuiz(GeneratedQuiz quiz) {
        quiz.getQuestions().forEach(question -> {
            question.setType(clean(question.getType()));
            question.setPrompt(clean(question.getPrompt()));
            question.setOptions(cleanList(question.getOptions()));
            question.setCorrectAnswer(clean(question.getCorrectAnswer()));
            question.setAcceptedAnswers(cleanList(question.getAcceptedAnswers()));
            question.setExplanation(clean(question.getExplanation()));
            question.setTopic(clean(question.getTopic()));
        });
    }

    private void validateAndSanitizeQuiz(GeneratedQuiz quiz, QuizRequest request) {
        if (quiz == null || quiz.getQuestions() == null || !validator.validate(quiz).isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE",
                    "The AI provider returned an invalid quiz. Please retry the request.");
        }
        sanitizeQuiz(quiz);
    }

    private String quizContext(QuizRequest request) {
        return String.join("\n",
                request.getTopic() == null ? "" : request.getTopic(),
                request.getSourceType() == null ? "" : request.getSourceType(),
                request.getSourceQuestion() == null ? "" : request.getSourceQuestion(),
                request.getSourceAnswer() == null ? "" : request.getSourceAnswer(),
                Boolean.toString(request.isShortAnswerEnabled()),
                request.getRepairHint() == null ? "" : request.getRepairHint());
    }

    private AiExecution<GeneratedQuiz> cachedQuizExecution(
            StudentProfile profile,
            java.util.UUID quizId,
            PromptTemplateService.Prompt prompt,
            GeneratedQuiz result) {
        result.setModelId(provider.modelId());
        result.setPromptVersion(prompt.version());
        result.setInputTokens(0);
        result.setOutputTokens(0);
        long remaining = quotas.remaining(profile.getProfileId()).getRemainingCallsToday();
        AiGenerationLog log = usageLogger.log(profile, null, quizId, result.getModelId(),
                prompt.version(), 0, 0, 0, true, "SUCCESS");
        return new AiExecution<>(result, log, remaining);
    }

    private GeneratedQuiz cacheValue(GeneratedQuiz result) {
        GeneratedQuiz cached = copyQuiz(result);
        cached.setModelId(null);
        cached.setPromptVersion(null);
        cached.setInputTokens(null);
        cached.setOutputTokens(null);
        return cached;
    }

    private GeneratedQuiz copyQuiz(GeneratedQuiz quiz) {
        return objectMapper.convertValue(quiz, GeneratedQuiz.class);
    }

    private void validateQuizOutputSafety(GeneratedQuiz quiz) {
        boolean unsafe = quiz.getQuestions().stream().anyMatch(question ->
                (question.getPrompt() + "\n" + question.getCorrectAnswer() + "\n"
                        + question.getExplanation() + "\n" + String.join("\n", safeList(question.getOptions()))
                        + "\n" + String.join("\n", safeList(question.getAcceptedAnswers())))
                        .matches("(?is).*\\b(how to build a bomb|how to poison|instructions to harm someone)\\b.*"));
        if (unsafe) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_BLOCKED_CONTENT",
                    "I can’t provide that quiz, but I can help with a safe academic topic.");
        }
    }

    private java.util.List<String> cleanList(java.util.List<String> values) {
        return safeList(values).stream().map(this::clean).toList();
    }

    private java.util.List<String> safeList(java.util.List<String> values) {
        return values == null ? java.util.List.of() : values;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\p{Cc}&&[^\\n\\t]]", "").trim();
    }

    private AiExecution<StructuredAnswer> cachedExecution(
            StudentProfile profile,
            AnswerRequest request,
            java.util.UUID questionId,
            PromptTemplateService.Prompt prompt,
            StructuredAnswer answer,
            boolean cacheHit) {
        answer.setModelId(provider.modelId());
        answer.setPromptVersion(prompt.version());
        answer.setInputTokens(0);
        answer.setOutputTokens(0);
        answer.setCacheHit(cacheHit);
        long remaining = quotas.remaining(profile.getProfileId()).getRemainingCallsToday();
        answer.setRemainingCallsToday(remaining);
        AiGenerationLog log = usageLogger.log(profile, questionId, null, answer.getModelId(),
                prompt.version(), 0, 0, 0, cacheHit, "SUCCESS");
        return new AiExecution<>(answer, log, remaining);
    }

    private StructuredAnswer cacheValue(StructuredAnswer answer) {
        StructuredAnswer cached = copyAnswer(answer);
        cached.setModelId(null);
        cached.setPromptVersion(null);
        cached.setInputTokens(null);
        cached.setOutputTokens(null);
        cached.setCacheHit(false);
        cached.setRemainingCallsToday(null);
        return cached;
    }

    private StructuredAnswer copyAnswer(StructuredAnswer answer) {
        return objectMapper.convertValue(answer, StructuredAnswer.class);
    }

    private StructuredAnswer awaitSharedAnswer(CompletableFuture<StructuredAnswer> pending) {
        try {
            return pending.join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Concurrent AI generation did not complete.", exception.getCause());
        }
    }

    private GeneratedQuiz awaitSharedQuiz(CompletableFuture<GeneratedQuiz> pending) {
        try {
            return pending.join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Concurrent AI quiz generation did not complete.", exception.getCause());
        }
    }

    private boolean allowedSubject(String subject) {
        return subject != null && ALLOWED_SUBJECTS.contains(subject.trim().toLowerCase(Locale.ROOT));
    }

    private long tokenCount(StructuredAnswer answer) {
        return (answer.getInputTokens() == null ? 0 : answer.getInputTokens())
                + (answer.getOutputTokens() == null ? 0 : answer.getOutputTokens());
    }

    private long tokenCount(GeneratedQuiz quiz) {
        return (quiz.getInputTokens() == null ? 0 : quiz.getInputTokens())
                + (quiz.getOutputTokens() == null ? 0 : quiz.getOutputTokens());
    }

    private String failureOutcome(RuntimeException exception) {
        return exception instanceof BusinessException business ? business.getCode() : "FAILURE";
    }

    private BusinessException invalidRequest() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AI_INPUT",
                "The question, subject, explanation style, or image is not valid.");
    }

    private BusinessException rateLimited(String message) {
        return new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMITED", message);
    }
}
