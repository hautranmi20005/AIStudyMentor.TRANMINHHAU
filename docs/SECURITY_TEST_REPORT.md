# P15 — Security verification report

**Status:** Verification completed with follow-up items  
**Branch:** `feature/p15-security`  
**Date:** 2026-11-16

This report records implementation and test evidence only. It does not claim legal compliance or a complete external penetration test.

## Findings and disposition

| ID | Severity | Status | Finding, owner, and decision |
|---|---|---|---|
| P15-01 | 🟡 MEDIUM | Fixed | Pending-consent checks could miss study routes containing matrix parameters. `GuardianConsentEnforcementFilter` now normalizes those path segments before matching. Owner: Backend. Decision: fixed and covered by filter tests. |
| P15-02 | 🟡 MEDIUM | Fixed | Upload writes checked attachment state before validating the signed upload capability, allowing an invalid token to distinguish a completed upload by response status. Capability validation now precedes state checks. Owner: Backend. Decision: fixed and covered by the IDOR integration matrix. |
| P15-03 | 🟡 MEDIUM | Fixed | `/auth/mfa/verify` did not use the configured MFA rate limit. The route now maps to the MFA policy. Owner: Backend. Decision: fixed and covered by the rate-limit test. |
| P15-04 | ⚪ LOW | Fixed | Unsupported request content types reached the generic exception handler and returned `500`. The handler now returns a sanitized `415 UNSUPPORTED_MEDIA_TYPE`. Owner: Backend. Decision: fixed and tested at unit and integration levels. |
| P15-05 | 🟡 MEDIUM | Open — follow-up | No application-wide JSON request-body size limit was found. Image uploads have a 10 MiB validation limit, but oversized generic JSON bodies are not bounded by an explicit application policy. Owner: Backend/API. Decision: track a separate request-size policy and enforce it for both content-length and streamed bodies; no global limit is claimed here. |
| P15-06 | 🟠 HIGH | Open — secret handling/history decision | The final Gitleaks history scan found 23 credential-like matches in reachable history: 20 in token-shaped API examples, two in the example MFA-key setting, and one in the ignored local environment file. The raw working-tree scan found 10 additional matches, all in the local `backend/.env`; no values were inspected or reproduced. Current tracked examples were sanitized, but historical blobs remain. Owner: Repository Security Owner. Decision: do not copy/commit these values; treat any value that was ever live as compromised and revoke/rotate it. No history rewrite without coordinated approval. See [SECURITY_REMEDIATION.md](./SECURITY_REMEDIATION.md). |
| P15-07 | 🟡 MEDIUM | Open — scan blocked | OWASP Dependency-Check could not update NVD data because no valid NVD API key was configured; it produced no dependency findings. A Gradle vulnerability scan was not completed. Owner: Security/Release Engineering. Decision: dependency status is unknown; do not mark the release dependency gate clear until Maven and Gradle scans complete with an available vulnerability database. |

No confirmed Critical finding was identified. P15-06 and P15-07 remain explicitly owned with a recorded decision; neither is represented as resolved.

## Verification matrix

### 1. IDOR

`BackendEndToEndIntegrationTest.idorMatrixRejectsForeignQuestionImageQuizAttemptReportAndDeviceResources` creates users A and B, then exercises A's token against B's question, answer report, bookmarks, image question, upload, quiz, attempt, and device-token resources. Protected resource routes return `404`/`403`; A's device-token deletion does not remove B's row. Signed upload/image content requests without B's capability do not return content.

The same integration test checks `%` and SQL-like search input against A's empty account while B owns matching data; no B rows are returned. It also authorizes an upload with a traversal-style file name and verifies the persisted storage path remains server-generated and user-scoped.

### 2. Authentication and authorization

- `SecurityRulesUnitTest` verifies expired JWT rejection, signature-tampering rejection, `alg=none` rejection, and MFA-pending token denial.
- Pending-consent coverage includes questions, quizzes, uploads, AI, and matrix-parameter path variants. Suspended/restricted status checks are included in security-rule tests.
- The end-to-end suite verifies refresh rotation and reuse rejection, logout refresh-token revocation, deletion re-authentication (wrong password and unverified MFA do not delete), and rejection of the deleted user's access token.
- Recovery-code reuse is rejected; account deletion remains idempotent.

### 3. Abuse controls

`ApiRateLimitFilterTest.enforcesConfiguredLimitsForEveryAbuseControlGroup` uses a one-request limit and verifies `429` for login, registration, refresh, MFA verification, recovery, AI generation, answer reports, and upload authorization. The MFA route mapping defect was fixed before this test passed.

`authenticatedQuestionSubmissionPersistsStructuredAiExplanation` exercises concurrent requests with the same idempotency key; both responses resolve to the same question. The MFA brute-force lockout test also passes.

These are deterministic automated integration/unit probes; no production endpoint or real account was used.

### 4. Input handling

- Oversized image payload validation, allowed image types, signed upload metadata, and local storage path validation are covered by `ImageContentValidatorTest` and `LocalStorageGatewayTest`.
- The IDOR integration test covers SQL/LIKE wildcard and SQL-like search strings, and traversal-style upload names.
- An integration test verifies `text/plain` on the JSON login route returns `415`, and JSON nested beyond the parser's depth limit returns a client error without exception details.
- `AiSafetyClassifierTest` and `PromptTemplateServiceTest` exercise prompt-injection samples. This is not a live-provider guarantee; no real Gemini prompt-injection session was run.
- No path was found that fetches a user-supplied URL server-side; the Gemini client uses a configured base URL. This is static code review, not an SSRF fuzzing campaign.
- Generic JSON request-body size is not explicitly bounded; see P15-05. No oversized generic JSON stress test was run.

### 5. Output handling

- `SecurityConfigCorsTest` verifies the explicit origin allow-list, rejects an unlisted preflight origin, and rejects wildcard/path origins.
- `apiResponsesIncludeDefaultSecurityHeaders` checks `X-Content-Type-Options`, `X-Frame-Options`, and `Cache-Control`.
- `GlobalExceptionHandlerTest` verifies error responses omit rejected password values and exception text. The unsupported-media response is now a sanitized 415.

### 6. Logs

The final backend end-to-end suite ran 15 tests. Its captured application/test output was scanned for JWT-shaped strings, bearer credentials, credential assignments, TOTP values, and known question text. All five pattern counts were zero. This is a targeted pattern scan, not proof that arbitrary secrets can never appear in logs.

### 7. Android

- `:app:testDebugUnitTest` and `:app:connectedDebugAndroidTest` passed; the connected suite passed 24 tests on the API 37 emulator, including `PrivacyAccountScreenTest.deleteRequiresTypedConfirmationAndReauthentication`.
- `:app:assembleRelease` succeeded with R8/minification and resource shrinking enabled. A temporary, locally generated signing key was used only for this scan build and removed afterward; the APK is not a production-signed release.
- The release APK was inspected with `aapt`; `allowBackup=false` is set and the release manifest references the main network-security configuration, whose base config disallows cleartext. The debug-only manifest is the only one that enables the emulator HTTP host.
- A byte/string scan of the release APK found no Google/Gemini key patterns, provider-token patterns, private-key markers, JWT-shaped values, or debug HTTP URL.
- `SecureTokenStorage` uses Android Keystore-backed `MasterKey` with AES-GCM values and AES-SIV preference keys. No token logging was found in the reviewed Android sources.
- Excluding sensitive screens from Android recents is not implemented. Decision: leave previews enabled as an optional UX choice; teams handling higher-sensitivity accounts may choose to enable `FLAG_SECURE`.

### 8. Dependency vulnerabilities

The requested Maven command, `mvn org.owasp:dependency-check-maven:check`, was attempted. It failed while updating NVD data with an invalid/empty API-key error before producing an analysis. No Critical/High dependency counts can therefore be stated. A Gradle dependency vulnerability scan was not completed. P15-07 is a release-gate follow-up, not a clean scan result.

### 9. Secret scan

Gitleaks v8.24.0 scanned the working tree and reachable Git history. The raw working-tree scan reported 10 `generic-api-key` matches, all in the ignored local `backend/.env`; the scan emitted no matches in other current files. No matched values were inspected or reproduced. The history scan reported 23 matches across five commits: API-spec examples, the example MFA-key setting, and a prior environment-file entry. Current tracked examples were sanitized, but Git history was not rewritten. The `.gitleaks.toml` contains an intended local-env allowlist, but this raw directory scan still reported the file; therefore this report does not call the whole worktree clean. Treat any historically live or otherwise exposed local value as compromised and follow [SECURITY_REMEDIATION.md](./SECURITY_REMEDIATION.md).

## Test evidence

- Backend: 46 targeted security tests passed, including 15 `BackendEndToEndIntegrationTest` cases.
- Android: connected instrumentation passed 24 tests; release APK build and static scan completed.
- Dependency scan: blocked before analysis by missing NVD API credentials.
- Gitleaks v8.24.0: 10 working-tree matches in ignored local `backend/.env`; 23 credential-like matches in reachable history. No values are included in this report.
