# ADR-0007: Private Image Storage and Signed Upload Authorization

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

Student question images may contain private educational or personal information. Upload authorization, object ownership, and image access must be enforced without exposing public storage objects or sending provider credentials to Android.

## 2. Decision Drivers & Alternatives Considered

- Keep uploaded images private and scope access to the owning user.
- Support Firebase Storage as Mode A and a backend-managed local-disk gateway as Mode B.
- Have the backend authorize uploads and mediate or validate access to stored objects.
- Alternatives considered: public object URLs, trusting client-supplied storage paths, and storing image bytes directly in question records.

## 3. Decision Outcome & Justification

Use private, user-authorized image storage. In Firebase mode, the backend issues upload authorization for Firebase Storage. In local mode, the backend provides the upload and retrieval gateway backed by local disk. A question may reference only images the authenticated user is authorized to attach; authorized image bytes may be supplied to the backend Gemini adapter.

The current implementation is incomplete: `/uploads/authorize` returns a generated storage path and random token rather than a signed, enforceable upload credential. Question submission initializes an empty attachment list, and the orchestration path does not retrieve or pass image bytes to Gemini. The adapter can encode provided image bytes as inline data, but that capability does not complete storage or attachment handling.

## 4. Consequences

- **Positive**: Private storage and server-side authorization reduce unauthorized image access and keep storage credentials off the Android client.
- **Negative**: Firebase and local-disk modes require distinct storage adapters, authorization checks, and cleanup behavior. Local disk is suitable only for the local runtime and does not provide durable shared storage.
- The current path/token response must not be treated as proof that an upload is authorized or signed.

## 5. References & Code Artifacts

- [QuestionController.java](../../backend/src/main/java/com/aistudymentor/question/controller/QuestionController.java)
- [QuestionService.java](../../backend/src/main/java/com/aistudymentor/question/service/QuestionService.java)
- [UploadAuthorizeResponse.java](../../backend/src/main/java/com/aistudymentor/question/dto/UploadAuthorizeResponse.java)
- [QuestionAttachment.java](../../backend/src/main/java/com/aistudymentor/question/entity/QuestionAttachment.java)
- [GeminiAdapterService.java](../../backend/src/main/java/com/aistudymentor/ai/service/GeminiAdapterService.java)
- [Question and attachment schema migration](../../backend/src/main/resources/db/migration/V3__Question_And_Ai_Schema.sql)
