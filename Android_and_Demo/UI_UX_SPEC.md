# UI/UX Specification v1.2 (Jetpack Compose & Material 3)

## 1. Navigation Flow & Architecture

The application implements a single-activity architecture powered by **Jetpack Navigation Compose**. The navigation hierarchy comprises three distinct graphs:

```
[Onboarding / Auth Graph]
  ├── Splash / AuthCheck
  ├── RegisterScreen (Age validation & conditional Guardian Email)
  ├── LoginScreen
  ├── MfaEnrollmentScreen (QR Code & Secret Setup)
  ├── MfaVerificationScreen (TOTP 6-digit challenge)
  └── GuardianConsentScreen (U13 Blocker with Status Polling)

[Main Bottom Navigation Graph]
  ├── AskAiScreen (CameraX & Text Prompt)
  │    └── AnswerDetailScreen (Step-by-step & Alternative Explanation)
  ├── QuizzesScreen
  │    ├── QuizTakingScreen (1 question/screen + progress bar)
  │    └── QuizResultScreen (Score & Server-graded review)
  ├── DashboardScreen (Metrics & Accuracy Charts)
  ├── HistoryScreen
  │    └── BookmarksScreen (Saved items)
  └── ProfileScreen (Student preferences & Settings)
```

---

## 2. Screen Specifications

### 2.1 Register Screen
* **Components**: Email, Password (max 72 bytes validation), Date of Birth (`DatePickerModal`), Education Level dropdown.
* **U13 Dynamic Behavior**: When selected DoB results in an age < 13 years (Decision D9), the `guardianEmail` field becomes mandatory with instructional copy: *"A parent or guardian must verify your account before you can ask study questions."*

### 2.2 Login Screen
* **Components**: Email, Password, Login CTA.
* **Routing**:
  - If backend returns `mfaRequired: true`: Navigates to `MfaVerificationScreen` passing `mfaSessionToken`.
  - If user is U13 with `accountStatus == PENDING_CONSENT`: Navigates directly to `GuardianConsentScreen`.
  - Otherwise: Navigates to Main Graph.

### 2.3 MFA Enrollment Screen (New)
* **Components**:
  - QR Code image generated from `otpAuthUri`.
  - Plaintext secret key with one-tap "Copy to Clipboard" button.
  - 6-digit confirmation code field (`OutlinedTextField` with numeric keyboard).
  - "Activate MFA" button.

### 2.4 MFA Verification Screen (Decision D1)
* **Components**:
  - 6-digit OTP input boxes with automatic focus traversal.
  - "Verify" CTA button.
  - **Important**: *No "resend code" button or countdown timer exists, as TOTP is generated locally by the student's authenticator app.*

### 2.5 Guardian Consent Screen (U13 Blocker)
* **Components**:
  - Informative banner explaining compliance requirements for students under 13.
  - Displays registered `guardianEmail`.
  - "Check Status" button (queries `GET /users/me/guardian-consent`).
  - "Resend Email" button (rate-limited, calls `POST /users/me/guardian-consent/resend`).

### 2.6 Profile Setup Screen (New)
* **Components**:
  - Education Grade Level (e.g., Grade 1 through 12, College).
  - Preferred Subjects (Chips: Mathematics, Physics, Chemistry, Biology, History).
  - Learning Style preferences (Visual, Step-by-step, Analogies).
  - Save Changes CTA.

### 2.7 Ask AI Screen
* **Components**:
  - CameraX live preview toggle with shutter button.
  - Gallery photo picker button.
  - Subject selector dropdown.
  - Text query input field with character counter.
  - "Solve Problem" elevated button.

### 2.8 Answer Detail Screen
* **Components**:
  - Problem summary card.
  - Vertical list of `ElevatedCard` components, each representing one step (`stepNumber`, `title`, `explanation`, KaTeX formatted `formula`).
  - "Final Answer" highlighted container with Material 3 PrimaryContainer color.
  - "Key Takeaways" bulleted chip group.
  - Floating Action Button (FAB) for Bookmark toggle.

### 2.9 Alternative Explanation Screen / View (FR-05)
* **Components**:
  - Accessible via "Explain Differently" button on the Answer Detail screen.
  - Displays pedagogical analogy or simplified conceptual breakdown.

### 2.10 Bookmarks Screen
* **Components**:
  - Tab within History screen displaying questions saved via `POST /questions/{id}/bookmark`.
  - Swipe-to-delete action that calls `DELETE /questions/{id}/bookmark`.

### 2.11 Practice Quiz Screens (FR-07)
* **Components**:
  - **QuizTakingScreen**: Linear progress bar at top (e.g., "Question 3 of 5"). Single question displayed per screen.
    - Multiple-Choice: Vertical RadioButtonGroup.
    - Short-Answer: `OutlinedTextField` for direct text entry.
    - "Next" and final "Submit Quiz" buttons.
  - **QuizResultScreen**: Celebration banner, score percentage card, list of question cards displaying whether the response was correct and server-side explanations.

### 2.12 Dashboard Screen (FR-08)
* **Components**:
  - Total Questions Answered stat card.
  - Overall Accuracy Gauge (Circular progress indicator).
  - Subject breakdown Bar Chart displaying accuracy rate per discipline.

---

## 3. UI States (Loading, Error & Empty)

* **Loading States**: Shimmer skeleton cards (`Brush.linearGradient`) applied to lists, question histories, and answer detail steps while awaiting Gemini network responses.
* **Error States**: Material 3 `Snackbar` or full-screen error illustrations with clear error descriptions (e.g., `MFA_REQUIRED`, `GUARDIAN_CONSENT_REQUIRED`, `AI_NOT_CONFIGURED`) and a "Try Again" retry action.
* **Empty States**: Friendly illustrations and call-to-action buttons for empty histories ("No questions asked yet! Tap the camera to start learning") and empty bookmarks.
