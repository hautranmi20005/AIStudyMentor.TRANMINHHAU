# ADR-0003: Under-13 Guardian Consent and Verification Email

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

Registration identifies users under 13 who require guardian consent before accessing core features. Verification must expire, be single-use, and avoid consuming a token when automated email scanners open a link.

## 2. Decision Drivers & Alternatives Considered

- Require guardian email and consent for users under 13.
- Use a cryptographically random, single-use verification token with a 72-hour expiry.
- Store a token hash rather than the raw token.
- Use Mailpit as the local-development SMTP sink.
- Alternatives considered: immediate activation, reusable or non-expiring links, and consuming consent through an email-link GET request.

## 3. Decision Outcome & Justification

Create a pending-consent account and send the guardian an email containing a 72-hour verification link. Store only the SHA-256 token hash and consume it on explicit POST approval or rejection. GET requests render a confirmation page without changing consent state. Local development routes mail to Mailpit; other environments use configured SMTP.

The existing service implements the random opaque token, hash, expiry, email flow, and non-consuming GET/POST confirmation behavior. Mailpit is defined in the development Compose stack. This workflow does not establish legal compliance; the legal review remains outstanding.

## 4. Consequences

- **Positive**: Expiry, one-time use, and hash-at-rest reduce risks from leaked or prefetched links. Mailpit supports local email-flow testing without sending real mail.
- **Negative**: Consent depends on email delivery and guardian action. Expired links require a resend flow.
- A technical consent workflow is not a substitute for legal review or jurisdiction-specific policy.

## 5. References & Code Artifacts

- [AuthService.java](../../backend/src/main/java/com/aistudymentor/auth/service/AuthService.java)
- [GuardianConsentService.java](../../backend/src/main/java/com/aistudymentor/auth/service/GuardianConsentService.java)
- [GuardianConsentController.java](../../backend/src/main/java/com/aistudymentor/auth/controller/GuardianConsentController.java)
- [development Compose stack](../../docker-compose.dev.yml)
- [Guardian consent schema migration](../../backend/src/main/resources/db/migration/V2__Fix_Core_Security.sql)
- [Known limitations](../KNOWN_LIMITATIONS.md)
