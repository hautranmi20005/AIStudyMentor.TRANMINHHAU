# Security and Privacy v1.1

1. **AI API Keys**: Must reside on backend. Never in APK.
2. **Guardian Consent**: U13 users are placed in `PENDING_CONSENT` state. All endpoints except Profile/Consent status return HTTP 403.
3. **MFA**: Required for JWT access token issuance. Rate-limited and audited.
4. **Data Ownership**: UUID-based profile checks on every database query.
5. **Firebase Rules**: Private bucket `users/{userId}/images/{imageId}`. Read/Write restricted to owner and Admin SDK.
6. **Quiz Integrity**: Server-side grading only.
