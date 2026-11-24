# P10 validation evidence

## Automated checks

| Check | Result |
| --- | --- |
| Backend `BackendEndToEndIntegrationTest` | Passed: 8 tests, 0 failures |
| Android `:app:testDebugUnitTest` (JDK 17) | Passed: 13 tests, 0 failures, 0 errors |
| Android `:app:connectedDebugAndroidTest` on Medium Phone API 37 (Android 17) | Passed: 14 tests, 0 failures. Includes account-flow, Room migration/DAO reconciliation, and queued-bookmark coverage. |

## Live backend and emulator journey

The local development stack was healthy during the live API/emulator checks. The first demo account was `demo.middle@local.test`; the second was `demo.high@local.test`.

| Acceptance check | Result |
| --- | --- |
| History paging with 25+ entries | Passed. Seeded 27 synthetic question/answer fixtures directly into the local MySQL database; the history list loaded additional pages. |
| Search and filters | Passed. Searched for `equation`; exercised subject, status, and bookmark filters through the live API. |
| Bookmark set/unset and idempotence | Passed. Repeated PUT produced the same set state; an offline unset was queued, then synced after reconnect. |
| Account isolation | Passed. The second account had no questions from the first account. After onboarding, its History screen showed “No history yet”; see [15-second-account-empty-history.png](./15-second-account-empty-history.png). |
| Sync cursor and deletions | Passed. The sync response included the deleted fixture ID as a tombstone; the client reconciled local data. |
| Offline reading | Passed. With airplane mode enabled and Wi-Fi disabled, synced history, detail/answer, and bookmarks remained readable. |
| Offline-only actions and pending indicator | Passed. Network-dependent actions were visibly disabled with an explanation; the offline bookmark displayed a pending-sync indicator. |
| Reconnect and persistence | Passed. The queued bookmark synced after network restoration; force-stop/reopen preserved local history. |
| Logout/account switch | Passed. Logout returned to sign-in; logging into the second account showed an empty history rather than the previous account's content. |

Screenshots for the journey are in this folder: [01-history-online.png](./01-history-online.png), [02-search-equation.png](./02-search-equation.png), [03-history-paging.png](./03-history-paging.png), [04-question-detail-online.png](./04-question-detail-online.png), [05-bookmark-set-online.png](./05-bookmark-set-online.png), [06-offline-detail.png](./06-offline-detail.png), [07-bookmark-pending-offline.png](./07-bookmark-pending-offline.png), [08-bookmarks-offline.png](./08-bookmarks-offline.png), [09-reconnected.png](./09-reconnected.png), [10-offline-disabled-actions.png](./10-offline-disabled-actions.png), [11-offline-quizzes.png](./11-offline-quizzes.png), [12-sync-after-airplane.png](./12-sync-after-airplane.png), [13-force-stop-reopen.png](./13-force-stop-reopen.png), [14-logout-account-cleanup.png](./14-logout-account-cleanup.png), and [15-second-account-empty-history.png](./15-second-account-empty-history.png).

## Limits

- The 27 acceptance fixtures were inserted directly into the local database; they were not created through the question-generation UI.
- No question fixture had an image, so offline thumbnail/image caching was not verified.
- The emulator was an API 37 virtual device; no physical-device verification was performed.
