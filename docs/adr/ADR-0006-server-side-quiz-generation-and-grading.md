# ADR-0006: Server-Side Quiz Generation and Deterministic Grading

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

Quiz answers and scoring must be authoritative on the backend. Sending correct answers before submission would let clients bypass assessment, while nondeterministic grading would make results difficult to explain or reproduce.

## 2. Decision Drivers & Alternatives Considered

- Support multiple-choice and fill-in-the-blank question formats.
- Keep correct answers server-side until the attempt is submitted.
- Grade submitted responses deterministically on the backend.
- Alternatives considered: client-side scoring, exposing answer keys in quiz DTOs, and accepting unvalidated arbitrary answer content.

## 3. Decision Outcome & Justification

Generate quizzes with MCQ and fill-in-the-blank questions. The backend owns question answers and grades submissions; pre-submission quiz DTOs omit `correct_answer`. Grading must be deterministic and return results only after submission.

The backend delegates quiz generation to `AiOrchestrator`, validates generated questions, keeps `correctAnswer` out of quiz-detail DTOs, and grades attempts server-side. Quiz subjects can be edited and quizzes deleted only before an attempt starts. Unit, mocked-provider MySQL/Redis integration, and focused Android emulator tests cover these paths. Live Gemini response quality and a complete live demo remain unverified.

## 4. Consequences

- **Positive**: Client tampering cannot directly determine scores, and repeated grading of the same stored quiz and responses is reproducible.
- **Negative**: Backend generation and grading logic require validation and test coverage; answer normalization rules must remain explicit and stable.
- Provider output can be rejected when it fails validation; live provider quality and behavior must be checked before release.

## 5. References & Code Artifacts

- [QuizService.java](../../backend/src/main/java/com/aistudymentor/quiz/service/QuizService.java)
- [QuizQuestionType.java](../../backend/src/main/java/com/aistudymentor/quiz/entity/QuizQuestionType.java)
- [QuizQuestionDto.java](../../backend/src/main/java/com/aistudymentor/quiz/dto/QuizQuestionDto.java)
- [Quiz schema migration](../../backend/src/main/resources/db/migration/V4__Quiz_Schema.sql)
- [API specification](../../Architecture_and_Engineering/API_SPEC.md)
