# ADR-0009: Data Retention and Account Deletion Cascade

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

Account deletion must remove the learner’s relational records and associated private assets rather than leave orphaned personal data. Cached data must also stop being accessible after deletion.

## 2. Decision Drivers & Alternatives Considered

- Hard-delete the account and its dependent relational data for the approved MVP policy.
- Remove associated Redis entries and stored image assets as part of the deletion workflow.
- Preserve ownership boundaries and avoid reporting success while cleanup is incomplete.
- Alternatives considered: soft deletion as the final retention policy, database-only deletion that leaves external data behind, and best-effort cleanup with no observable failure handling.

## 3. Decision Outcome & Justification

Use hard deletion with relational cascade for user-owned records, and include the user’s Redis keys and image assets in the deletion workflow. Database deletion must be transactional. Redis and object storage are external systems and cannot participate in the same database transaction; their cleanup must therefore be coordinated and failures surfaced or retried rather than described as an atomic cross-system transaction.

The current schema uses foreign-key cascades and JPA cascading for several dependent records. The inspected backend does not expose an account-deletion endpoint or a service that coordinates deletion of Redis entries and stored images. The approved complete deletion workflow is not yet implemented.

## 4. Consequences

- **Positive**: Hard deletion limits retention of user data and relational cascades reduce orphan records.
- **Negative**: Cleanup across MySQL, Redis, and storage cannot be made atomic with a single database transaction. A failure in an external cleanup step requires explicit error handling and retry/reconciliation.
- Image-storage cleanup and Redis key ownership must be defined before the workflow can guarantee completion.

## 5. References & Code Artifacts

- [User.java](../../backend/src/main/java/com/aistudymentor/auth/entity/User.java)
- [QuestionAttachment.java](../../backend/src/main/java/com/aistudymentor/question/entity/QuestionAttachment.java)
- [Core security schema migration](../../backend/src/main/resources/db/migration/V1__Init_Core_Security_Schema.sql)
- [Question and AI schema migration](../../backend/src/main/resources/db/migration/V3__Question_And_Ai_Schema.sql)
- [QuestionService.java](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java)
- [Repository audit](../audit/PHASE_0_REPOSITORY_AUDIT.md)
