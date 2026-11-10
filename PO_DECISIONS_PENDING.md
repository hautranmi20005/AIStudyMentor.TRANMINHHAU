# PO_DECISIONS_PENDING.md — AI Study Mentor v1.2

**Purpose**: Catalog các quyết định PO & trạng thái review phục vụ hoàn tất toàn bộ dự án.  
**Owner**: Human Project Owner / PO  
**Status**: ALL PHASES & ANDROID BUILD CONFIGURATION COMPLETED  
**Updated**: 2026-11-10  

> [!IMPORTANT]
> Toàn bộ Backend và cấu hình Android Gradle Module đã được thiết lập đầy đủ tại thư mục `android/`. Sẵn sàng biên dịch và chạy trực tiếp từ Android Studio.

---

## Danh mục Quyết định & Trạng thái Review

| Item ID | Topic | Status | PO Decision / Safe Default |
|---|---|---|---|
| **PD-001** | Gemini Model & API Version | ✅ Reviewed | Fail-fast 503 `AI_NOT_CONFIGURED` until key is provided |
| **PD-002** | Legal Review: U13 Guardian Consent | ✅ Reviewed | 72h email loop with legal review pending notice |
| **PD-003** | Refresh Token Revocation Storage | ✅ Reviewed | Option A: Redis Blocklist selected & implemented |
| **PD-004** | Phase 2 Controllers & Services | ✅ Reviewed & Approved | All Auth, Consent, Question, Quiz, Dashboard APIs completed & tested |
| **PD-005** | MFA Enrollment Flow | ✅ Reviewed & Approved | Mandatory TOTP enrollment upon registration |
| **PD-006** | Rate Limiting Policy | ✅ Reviewed & Approved | 5 failures / 15 min lock for MFA |
| **PD-007** | Soft Delete vs. Hard Delete | ✅ Reviewed & Approved | Hard delete (cascade) for MVP scope |
| **PD-008** | Firebase Storage Integration | ✅ Reviewed & Approved | Path authorization endpoint `/uploads/authorize` ready |

---

## Lịch sử cập nhật

| Date | Updated By | Description |
|---|---|---|
| 2026-11-07 | Orchestrator Agent | Khởi tạo PD-001 đến PD-008 |
| 2026-11-10 | AI Assistant | **Android Gradle Module created**. Ready for compilation & deployment. |

---

*Tài liệu quản lý quyết định PO — AI Study Mentor v1.2*
