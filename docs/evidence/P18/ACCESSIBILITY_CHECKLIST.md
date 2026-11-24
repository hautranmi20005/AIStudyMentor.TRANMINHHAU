# P18 Accessibility and Layout Check

## Verified

- Android `lintDebug` completed successfully; the final lint XML has **0 `HardcodedText` findings**. English and Vietnamese each contain 188 string resources; key parity is checked during closeout.
- Palette contrast was calculated for the primary text/background and button color pairs:

  | Pair | Contrast |
  |---|---:|
  | Light body text / background | 16.21:1 |
  | Light primary button label / primary | 7.19:1 |
  | Light secondary button label / secondary | 6.51:1 |
  | Light tertiary button label / tertiary | 6.48:1 |
  | Dark body text / background | 14.30:1 |
  | Dark primary button label / primary | 7.76:1 |
  | Dark secondary button label / secondary | 7.53:1 |
  | Dark tertiary button label / tertiary | 7.93:1 |

- The quota refresh control explicitly reserves a 48dp target. Other buttons, text fields, and radio controls use Material 3 components and their standard interactive sizing; a full-app target-size measurement has not been run.
- Navigational, back, refresh, and image content has named descriptions. Decorative icons inside text-labelled actions/rows are intentionally excluded from TalkBack announcements.
- Ask and Quiz entry screens were captured and visually checked at 200% font scale. Their text remains readable; content below the viewport remains scrollable.
- Ask and Quiz layouts were exercised at 480x800 (compact) and 1280x800 (tablet-sized) emulator configurations. The main content is width-constrained and centered on the wider configuration.

## Not fully verified

- A full TalkBack traversal/session and automated Compose accessibility-checker run were not performed. The current Android test dependencies do not include a dedicated accessibility-checker library.
- The color ratios above cover the selected palette pairs, not every component state, disabled control, image, or dynamically supplied value. Disabled controls are visually de-emphasized by design.
- The 200% font-scale visual check covers Ask and Quiz entry screens, not every screen or every error/dialog state.
- The target-size check was not exhaustively measured across every clickable element.

## Screenshot evidence

- Default phone, light/dark: [Ask light](./screenshots/p18-ask-light.png), [Ask dark](./screenshots/p18-ask-dark.png), [Quiz light](./screenshots/p18-quiz-light.png), [Quiz dark](./screenshots/p18-quiz-dark.png).
- 200% font scale: [Ask light](./font-200/screenshots/p18-ask-light.png), [Quiz light](./font-200/screenshots/p18-quiz-light.png).
- Tablet-sized: [Ask](./tablet/screenshots/p18-ask-light.png), [Quiz](./tablet/screenshots/p18-quiz-light.png).
- Compact: [Ask](./small-screen/screenshots/p18-ask-light.png), [Quiz](./small-screen/screenshots/p18-quiz-light.png).

Screenshots use fake repositories and show the expected unavailable-quota state rather than live account data.
