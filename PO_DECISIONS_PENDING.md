# PO_DECISIONS_PENDING.md — AI Study Mentor v1.2

**Purpose**: Các quyết định chưa được PO chỉ định. Mỗi mục đều có implementation an toàn nhất đang được áp dụng mặc định.  
**Owner**: Human Project Owner  
**Updated**: 2026-11-07  

> [!IMPORTANT]
> Orchestrator Agent KHÔNG tự ý quyết định các mục này. Triển khai hiện tại chọn **safe default** (option ít rủi ro nhất, ẩn sau cấu hình) cho đến khi PO chỉ định rõ.

---

## Mục cần quyết định

### PD-001 — Gemini Model & API Version
**Context**: D3 yêu cầu không có giá trị mặc định trong code, yml hoặc .env.example.  
**Current state**: `.env.example` có `GEMINI_MODEL=<SET_ME>` và `GEMINI_API_VERSION=<SET_ME>`.  
Ứng dụng khởi động được nhưng mọi lệnh gọi AI sẽ fail fast với `AI_NOT_CONFIGURED` (503).  
**PO action required**:  
Cung cấp giá trị cho:
```
GEMINI_MODEL=gemini-?????         # e.g., gemini-1.5-flash
GEMINI_API_VERSION=v?????         # e.g., v1beta
```
**Unblocks**: Toàn bộ luồng Q&A và Quiz AI.

---

### PD-002 — Legal Review: Guardian Consent Email Loop (D2)
**Context**: D2 đã được implement (email loop, token hash, 72h, audit fields, Mailpit dev).  
**Caveat**: Tài liệu đã được đánh dấu rõ: **"legal review pending — not production-ready"**.  
**PO action required**:
- Xác nhận đã có legal review về COPPA (Mỹ) và GDPR-K (EU) trước khi deploy lên production.
- Nếu cần sửa flow (ví dụ: thêm chữ ký số, lưu trữ bổ sung), cung cấp spec chi tiết.

**Unblocks**: Production deployment của tính năng đăng ký U13.

---

### PD-003 — Refresh Token Revocation Storage (H2)
**Context**: JTI đã được thêm vào tất cả token (access, refresh, MFA session). Tuy nhiên, để revocation thực sự hoạt động (logout, bị steal token), cần một store để lưu JTI của token đã bị thu hồi.  
**Two options**:

| Option | Pros | Cons |
|---|---|---|
| **A — Redis Blocklist** (safe default assumed) | Low latency; TTL tự dọn; đã có Redis | Nếu Redis down → revocation không hoạt động |
| **B — DB `refresh_tokens` table** | Bền vững; audit history; ACID | Thêm V5 migration; query chậm hơn |

**Current safe default**: Implementation skeleton cho Redis blocklist đã được chuẩn bị nhưng chưa wire vào controller (vì stop condition sau APPROVED).  
**PO action required**: Chọn Option A hoặc B.  
**Unblocks**: Implement `POST /auth/refresh` và `POST /auth/logout` endpoints (Phase 2).

---

### PD-004 — Phase 2 Controller & Service Implementation
**Context**: WP1–WP6 đã xong. Hiện tại chỉ có Security filters, JPA entities, repositories và một số service (AccountStatusService, MfaRateLimitService, GeminiAdapterService).  
**Chưa có**:
- `AuthController`, `AuthService` (register, login, MFA verify, refresh, logout)
- `GuardianConsentService`, `GuardianConsentController`
- `QuestionController`, `QuestionService`
- `QuizController`, `QuizService`
- `ProfileController`, `ProfileService`

**PO action required**: Confirm "APPROVED to implement Phase 2 — Auth & Consent controllers/services" để Backend Lead bắt đầu.

---

### PD-005 — MFA Enrollment Flow: First Login vs. Registration
**Context**: API_SPEC.md v1.2 mô tả TOTP enrollment sau khi đăng ký thành công.  
**Ambiguity**: Hiện chưa rõ:
- Enrollment bắt buộc ngay trong luồng đăng ký (không thể skip)?
- Hay user có thể login không có MFA và enable sau?

**Current safe default**: Enrollment bắt buộc (ngay sau register), skip = không được phép.  
**PO action required**: Confirm hoặc chỉnh sửa.  
**Unblocks**: `POST /auth/mfa/enroll`, `POST /auth/mfa/verify` logic.

---

### PD-006 — Rate Limiting: Global vs. Per-User-Per-Endpoint
**Context**: `MfaRateLimitService` implement 5 failures / 15 min cho MFA verify.  
**Ambiguity**: Các endpoint khác (login, register, Q&A) chưa có rate limit spec.  
**PO action required**: Cung cấp rate limit spec cho:

| Endpoint | Suggested Default | Confirm? |
|---|---|---|
| `POST /auth/register` | 5 req/hour per IP | ❓ |
| `POST /auth/login` | 10 req/min per IP | ❓ |
| `POST /questions` | 20 req/hour per user | ❓ |
| `POST /quizzes` | 10 req/hour per user | ❓ |

**Current safe default**: Chưa implement rate limit cho các endpoint trên (safe = không block user, but no protection). Sẽ implement sau khi PO confirm.

---

### PD-007 — Soft Delete vs. Hard Delete cho User / Question
**Context**: Schema hiện tại không có cột `deleted_at` hoặc `is_deleted`.  
**Ambiguity**: Nếu user hoặc question bị xóa, behavior là gì?  
**Current safe default**: Hard delete (cascade). Nếu cần soft delete, cần V5 migration.  
**PO action required**: Confirm hard delete là đủ cho MVP scope.

---

### PD-008 — Firebase Storage Integration Timing
**Context**: DATABASE_DESIGN.md và TECH_STACK.md đề cập Firebase Storage cho `question_attachments`.  
**Ambiguity**: Tích hợp Firebase Storage có nằm trong Phase 1 không, hay là Phase 2+?  
**Current safe default**: Phase 2+ (không làm ở Phase 1).  
**PO action required**: Confirm timing.

---

## Lịch sử cập nhật

| Date | Added By | Item |
|---|---|---|
| 2026-11-07 | Orchestrator Agent | PD-001 đến PD-008 (khởi tạo) |

---

*File này sẽ được cập nhật mỗi khi Agent gặp quyết định chưa được chỉ định. PO xóa mục sau khi đã quyết định và update tài liệu tương ứng.*
