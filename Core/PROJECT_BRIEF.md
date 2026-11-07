# Project Brief: AI Study Mentor (v1.2)

**Objective**: Deliver a secure, Android-first AI tutoring application that uses Google Gemini to explain academic concepts step-by-step, while offering practice quizzes to assess learning progress.

**Target Audience**: Students across grade levels (including Under-13 students requiring verifiable parental/guardian consent).

**Key Differentiators & Invariants**:
1. **Safety & Compliance First**: TOTP Multi-Factor Authentication (RFC 6238), strict student profile data ownership, under-13 consent blocks with 72-hour email loop verification, and server-side secret isolation.
2. **Pedagogical Structured AI**: Not a free-form chatbot. All explanations strictly adhere to `docs/ai_response.schema.json`, providing sequenced steps with KaTeX formulas, final answers, and key takeaways.
3. **Assessment & Measurement**: Practice quizzes with strictly server-side evaluation (answers never leaked to client pre-submission) and progress dashboards tracking subject accuracy.
