# ADR-0010: Idempotent Bookmarks and Question Submissions

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

Mobile clients can retry requests after timeouts or connectivity loss. A retry must not reverse a bookmark action or create duplicate AI-generation work when the original request succeeded but its response was lost.

## 2. Decision Drivers & Alternatives Considered

- Use `PUT` to add or ensure a bookmark and `DELETE` to remove it.
- Require an `Idempotency-Key` header for AI-generation submissions and associate repeated keys with the original operation/result.
- Scope idempotency records to the authenticated user and operation.
- Alternatives considered: toggle semantics, treating every retry as a new generation, and relying only on client-side retry suppression.

## 3. Decision Outcome & Justification

Bookmark addition is an idempotent `PUT`; bookmark removal is an idempotent `DELETE`. AI question-generation submissions require an `Idempotency-Key` so a retry of the same operation returns the original result rather than creating duplicate questions, answers, or provider calls.

The bookmark controller now exposes an idempotent `POST` add and `DELETE` remove: repeating either operation leaves the requested state unchanged. The add route remains `POST` rather than the decision's `PUT`. Question creation is still `POST` without an idempotency header or deduplication record, so the complete decision is only partially implemented.

## 4. Consequences

- **Positive**: Retries become safe for clients, reducing duplicate records and unnecessary provider calls.
- **Negative**: The backend must persist or otherwise reliably coordinate idempotency keys, define their retention period, reject conflicting reuse, and ensure key ownership.
- Client and API documentation must agree on the operation verbs and retry contract.

## 5. References & Code Artifacts

- [QuestionController.java](../../backend/src/main/java/com/aistudymentor/question/controller/QuestionController.java)
- [QuestionService.java](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java)
- [QuestionCreateRequest.java](../../backend/src/main/java/com/aistudymentor/question/dto/QuestionCreateRequest.java)
- [API specification](../../Architecture_and_Engineering/API_SPEC.md)
- [Question schema migration](../../backend/src/main/resources/db/migration/V3__Question_And_Ai_Schema.sql)
