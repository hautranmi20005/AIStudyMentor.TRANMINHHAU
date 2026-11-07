# API Specification v1.2

**Base Path**: `/api/v1`

All responses are formatted in `application/json;charset=UTF-8`. All timestamps follow ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss'Z'`).

---

## 1. Global Standards

### 1.1 Standard Error Response (Decision D5)
```json
{
  "status": 403,
  "error": "FORBIDDEN",
  "code": "GUARDIAN_CONSENT_REQUIRED",
  "message": "Guardian consent is required before accessing study features.",
  "timestamp": "2026-11-07T16:30:00Z",
  "path": "/api/v1/questions"
}
```

#### Error Code Mapping:
* **401 Unauthorized**:
  - `UNAUTHORIZED`: Missing `Authorization: Bearer` header or signature validation failure.
  - `TOKEN_EXPIRED`: JWT token expiration timestamp is in the past.
  - `INVALID_TOKEN`: Malformed token string.
* **403 Forbidden**:
  - `MFA_REQUIRED`: An `MFA_SESSION` temporary token was submitted to a protected endpoint.
  - `GUARDIAN_CONSENT_REQUIRED`: Under-13 account with `account_status == PENDING_CONSENT`.
  - `ACCOUNT_SUSPENDED`: Account with `account_status == SUSPENDED`.
  - `FORBIDDEN`: Access denied to a resource owned by another profile (IDOR protection).
* **429 Too Many Requests**:
  - `MFA_RATE_LIMITED`: More than 5 failed TOTP attempts within 15 minutes.
* **503 Service Unavailable**:
  - `AI_NOT_CONFIGURED`: Google Gemini model/API keys are unconfigured (Decision D3).

### 1.2 Standard Pagination Response
Used for all paginated listing endpoints:
```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8,
  "isLast": false
}
```

---

## 2. Authentication & Guardian Consent

### 2.1 Register Student Account
* **Method**: `POST`
* **Path**: `/auth/register`
* **Auth**: Public
* **Request**:
  ```json
  {
    "email": "student@example.com",
    "password": "SecurePassword123",
    "dob": "2015-06-15",
    "educationLevel": "Grade 5",
    "guardianEmail": "parent@example.com"
  }
  ```
  *(Validation: `guardianEmail` is mandatory if `dob` indicates age < 13; password max 72 bytes).*
* **Response (201 Created)**:
  ```json
  {
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "student@example.com",
    "accountStatus": "PENDING_CONSENT",
    "consentStatus": "PENDING",
    "mfaRequired": false,
    "guardianConsentRequired": true,
    "message": "Registration successful. Guardian consent required before accessing core features."
  }
  ```
* **Errors**: `400 BAD_REQUEST` (VALIDATION_FAILED, EMAIL_ALREADY_EXISTS).

### 2.2 Login
* **Method**: `POST`
* **Path**: `/auth/login`
* **Auth**: Public
* **Request**:
  ```json
  {
    "email": "student@example.com",
    "password": "SecurePassword123"
  }
  ```
* **Response (200 OK - MFA Enrolled)**:
  ```json
  {
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "student@example.com",
    "mfaRequired": true,
    "mfaSessionToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 300
  }
  ```
* **Response (200 OK - No MFA Enrolled yet)**:
  ```json
  {
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "student@example.com",
    "mfaRequired": false,
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
  ```
* **Errors**: `401 UNAUTHORIZED` (INVALID_CREDENTIALS).

### 2.3 Enroll MFA (TOTP)
* **Method**: `POST`
* **Path**: `/auth/mfa/enroll`
* **Auth**: Bearer ACCESS token
* **Response (200 OK)**:
  ```json
  {
    "factorId": "4c985f64-5717-4562-b3fc-2c963f66afa7",
    "type": "TOTP",
    "secret": "JBSWY3DPEHPK3PXP",
    "otpAuthUri": "otpauth://totp/AIStudyMentor:student@example.com?secret=JBSWY3DPEHPK3PXP&issuer=AIStudyMentor"
  }
  ```

### 2.4 Activate MFA
* **Method**: `POST`
* **Path**: `/auth/mfa/activate`
* **Auth**: Bearer ACCESS token
* **Request**:
  ```json
  {
    "factorId": "4c985f64-5717-4562-b3fc-2c963f66afa7",
    "code": "123456"
  }
  ```
* **Response (200 OK)**:
  ```json
  {
    "verified": true,
    "message": "TOTP MFA factor successfully activated and verified."
  }
  ```
* **Errors**: `400 BAD_REQUEST` (INVALID_TOTP_CODE).

### 2.5 Verify MFA (Login Challenge)
* **Method**: `POST`
* **Path**: `/auth/mfa/verify`
* **Auth**: Public (Requires `mfaSessionToken` in payload)
* **Request**:
  ```json
  {
    "mfaSessionToken": "eyJhbGciOiJIUzI1NiJ9...",
    "code": "123456",
    "type": "TOTP"
  }
  ```
* **Response (200 OK)**:
  ```json
  {
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "student@example.com",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
  ```
* **Errors**: `401 UNAUTHORIZED` (INVALID_TOTP_CODE), `429 TOO_MANY_REQUESTS` (MFA_RATE_LIMITED).

### 2.6 Rotate Refresh Token
* **Method**: `POST`
* **Path**: `/auth/refresh`
* **Auth**: Public (Requires valid Refresh Token)
* **Request**:
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```
* **Response (200 OK)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
  ```
* **Errors**: `401 UNAUTHORIZED` (TOKEN_EXPIRED, INVALID_TOKEN).

### 2.7 Logout (Revoke Refresh Token)
* **Method**: `POST`
* **Path**: `/auth/logout`
* **Auth**: Bearer ACCESS token
* **Request**:
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```
* **Response (200 OK)**:
  ```json
  {
    "message": "Logged out successfully. Refresh token revoked."
  }
  ```

### 2.8 Get Guardian Consent Status
* **Method**: `GET`
* **Path**: `/users/me/guardian-consent`
* **Auth**: Bearer ACCESS token (Allowed for `PENDING_CONSENT` accounts)
* **Response (200 OK)**:
  ```json
  {
    "consentId": "6ea85f64-5717-4562-b3fc-2c963f66afa9",
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "guardianEmail": "parent@example.com",
    "status": "PENDING",
    "verifiedAt": null,
    "canAccessCoreFeatures": false
  }
  ```

### 2.9 Resend Guardian Consent Email
* **Method**: `POST`
* **Path**: `/users/me/guardian-consent/resend`
* **Auth**: Bearer ACCESS token (Allowed for `PENDING_CONSENT` accounts; rate-limited)
* **Response (200 OK)**:
  ```json
  {
    "message": "Verification email resent successfully.",
    "resendCount": 1
  }
  ```
* **Errors**: `429 TOO_MANY_REQUESTS` (RESEND_LIMIT_EXCEEDED).

### 2.10 Verify Guardian Consent (Email Link Webhook)
* **Method**: `GET`
* **Path**: `/guardian-consent/verify?token=...`
* **Auth**: Public
* **Response (200 OK)**:
  ```json
  {
    "verified": true,
    "status": "APPROVED",
    "message": "Guardian consent approved. Student account is now ACTIVE."
  }
  ```
* **Errors**: `400 BAD_REQUEST` (TOKEN_EXPIRED, INVALID_TOKEN).

---

## 3. Student Profile

### 3.1 Get Profile
* **Method**: `GET`
* **Path**: `/users/me/profile`
* **Auth**: Bearer ACCESS token (Allowed for `PENDING_CONSENT` accounts)
* **Response (200 OK)**:
  ```json
  {
    "profileId": "7fa85f64-5717-4562-b3fc-2c963f66afb0",
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "educationLevel": "Grade 5",
    "preferredSubject": "Mathematics",
    "learningStyle": "Visual"
  }
  ```

### 3.2 Update Profile
* **Method**: `PUT`
* **Path**: `/users/me/profile`
* **Auth**: Bearer ACCESS token (Allowed for `PENDING_CONSENT` accounts)
* **Request**:
  ```json
  {
    "educationLevel": "Grade 6",
    "preferredSubject": "Physics",
    "learningStyle": "Step-by-step"
  }
  ```
* **Response (200 OK)**: Updated Profile DTO.

---

## 4. Question & AI Explanations

### 4.1 Authorize Firebase Upload
* **Method**: `POST`
* **Path**: `/uploads/authorize`
* **Auth**: Bearer ACCESS token
* **Request**:
  ```json
  {
    "fileName": "problem_photo.jpg",
    "contentType": "image/jpeg"
  }
  ```
* **Response (200 OK)**:
  ```json
  {
    "firebaseStoragePath": "users/3fa85f64-5717-4562-b3fc-2c963f66afa6/images/uuid_problem_photo.jpg",
    "uploadToken": "temp_auth_token"
  }
  ```

### 4.2 Submit Question
* **Method**: `POST`
* **Path**: `/questions`
* **Auth**: Bearer ACCESS token (Blocked for `PENDING_CONSENT` -> 403)
* **Request**:
  ```json
  {
    "subject": "Mathematics",
    "questionText": "Solve for x: 3x + 7 = 22",
    "imageStoragePath": null
  }
  ```
* **Response (201 Created)**:
  ```json
  {
    "questionId": "8fa85f64-5717-4562-b3fc-2c963f66afb1",
    "subject": "Mathematics",
    "questionText": "Solve for x: 3x + 7 = 22",
    "status": "ANSWERED",
    "isBookmarked": false,
    "attachments": [],
    "answer": {
      "answerId": "9fa85f64-5717-4562-b3fc-2c963f66afb2",
      "isAlternative": false,
      "structuredExplanation": {
        "summary": "This is a linear equation with one variable. Isolate x by subtracting 7 and dividing by 3.",
        "steps": [
          {
            "stepNumber": 1,
            "title": "Subtract 7 from both sides",
            "explanation": "Subtract 7 from both sides to isolate the term containing x.",
            "formula": "3x = 22 - 7 \\Rightarrow 3x = 15"
          },
          {
            "stepNumber": 2,
            "title": "Divide by 3",
            "explanation": "Divide both sides by 3 to find the value of x.",
            "formula": "x = \\frac{15}{3} = 5"
          }
        ],
        "finalAnswer": "x = 5",
        "keyTakeaways": [
          "Whatever operation is performed on one side of an equation must be performed on the other.",
          "Inverse of addition is subtraction; inverse of multiplication is division."
        ],
        "alternativeExplanation": null
      },
      "createdAt": "2026-11-07T16:31:00Z"
    },
    "createdAt": "2026-11-07T16:30:58Z"
  }
  ```
* **Errors**: `403 FORBIDDEN` (GUARDIAN_CONSENT_REQUIRED), `503 SERVICE_UNAVAILABLE` (AI_NOT_CONFIGURED).

### 4.3 Request Alternative Explanation (FR-05)
* **Method**: `POST`
* **Path**: `/questions/{id}/alternative-explanation`
* **Auth**: Bearer ACCESS token
* **Response (200 OK)**: AnswerResponse with `isAlternative: true` and populated `alternativeExplanation`.

### 4.4 Get Single Question
* **Method**: `GET`
* **Path**: `/questions/{id}`
* **Auth**: Bearer ACCESS token
* **Response (200 OK)**: Detailed `QuestionResponse`.

### 4.5 List Question History (Paginated)
* **Method**: `GET`
* **Path**: `/questions?page=0&size=20`
* **Auth**: Bearer ACCESS token
* **Response (200 OK)**: Standard pagination containing `QuestionSummaryResponse` items.

### 4.6 Bookmark Question
* **Method**: `POST`
* **Path**: `/questions/{id}/bookmark`
* **Auth**: Bearer ACCESS token
* **Response (200 OK)**:
  ```json
  {
    "questionId": "8fa85f64-5717-4562-b3fc-2c963f66afb1",
    "isBookmarked": true,
    "bookmarkedAt": "2026-11-07T16:32:00Z"
  }
  ```

### 4.7 Remove Bookmark
* **Method**: `DELETE`
* **Path**: `/questions/{id}/bookmark`
* **Auth**: Bearer ACCESS token
* **Response (200 OK)**:
  ```json
  {
    "questionId": "8fa85f64-5717-4562-b3fc-2c963f66afb1",
    "isBookmarked": false,
    "bookmarkedAt": null
  }
  ```

---

## 5. Practice Quizzes & Progress Dashboard

### 5.1 Generate Practice Quiz
* **Method**: `POST`
* **Path**: `/quizzes`
* **Auth**: Bearer ACCESS token
* **Request**:
  ```json
  {
    "subject": "Mathematics",
    "numQuestions": 5
  }
  ```
* **Response (201 Created)**: Returns Quiz metadata with generated questions (Options included, **correct answers stripped**).

### 5.2 List Quizzes (Paginated)
* **Method**: `GET`
* **Path**: `/quizzes?page=0&size=20`
* **Auth**: Bearer ACCESS token

### 5.3 Get Quiz Detail
* **Method**: `GET`
* **Path**: `/quizzes/{id}`
* **Auth**: Bearer ACCESS token
* **Response (200 OK)**:
  ```json
  {
    "quizId": "afb85f64-5717-4562-b3fc-2c963f66afc1",
    "subject": "Mathematics",
    "status": "GENERATED",
    "questions": [
      {
        "questionId": "bfb85f64-5717-4562-b3fc-2c963f66afc2",
        "type": "MCQ",
        "prompt": "What is 7 multiplied by 8?",
        "options": [
          {"optionId": "c1", "optionText": "54", "optionOrder": 1},
          {"optionId": "c2", "optionText": "56", "optionOrder": 2},
          {"optionId": "c3", "optionText": "64", "optionOrder": 3}
        ]
      }
    ]
  }
  ```
  *(Security Assertion: `correct_answer` is strictly omitted from this payload).*

### 5.4 Start Quiz Attempt
* **Method**: `POST`
* **Path**: `/quizzes/{id}/attempts`
* **Auth**: Bearer ACCESS token
* **Response (201 Created)**: Returns `attemptId` and `startTime`.

### 5.5 Submit Quiz Attempt & Server-Side Grade (FR-07)
* **Method**: `POST`
* **Path**: `/quizzes/{id}/attempts/{attemptId}/submit`
* **Auth**: Bearer ACCESS token
* **Request**:
  ```json
  {
    "responses": [
      {
        "questionId": "bfb85f64-5717-4562-b3fc-2c963f66afc2",
        "selectedOptionId": "c2",
        "subjectiveText": null
      }
    ]
  }
  ```
* **Response (200 OK)**:
  ```json
  {
    "attemptId": "dfb85f64-5717-4562-b3fc-2c963f66afc3",
    "score": 100,
    "totalQuestions": 1,
    "correctCount": 1,
    "results": [
      {
        "questionId": "bfb85f64-5717-4562-b3fc-2c963f66afc2",
        "isCorrect": true,
        "correctAnswer": "56",
        "explanation": "7 * 8 = 56"
      }
    ]
  }
  ```

### 5.6 Get Progress Dashboard Metrics (FR-08)
* **Method**: `GET`
* **Path**: `/dashboard/metrics`
* **Auth**: Bearer ACCESS token
* **Response (200 OK)**:
  ```json
  {
    "totalQuestionsAsked": 42,
    "totalQuizzesCompleted": 12,
    "overallAccuracyRate": 85.5,
    "subjectBreakdown": [
      {
        "subject": "Mathematics",
        "questionsCount": 25,
        "quizCount": 8,
        "accuracyRate": 90.0
      },
      {
        "subject": "Physics",
        "questionsCount": 17,
        "quizCount": 4,
        "accuracyRate": 78.2
      }
    ]
  }
  ```
