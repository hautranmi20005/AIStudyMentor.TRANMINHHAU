# P09 photo-question verification

## Verified on `emulator-5554`

- `10-camera-preview.png`, `11-camera-captured.png`: CameraX permission, virtual-scene preview, and capture. The virtual scene did not contain the test equation, so the final question used the Photo Picker.
- `12-photo-question-ready.png`, `13-upload-progress.png`: selected `3x - 7 = 11` with the prompt “Solve the problem in the image.” and observed upload progress. After the upload, the UI briefly showed a network error although the backend had persisted the answer; Retry then opened the successful answer.
- `14-answer-with-image.png`, `15-answer-final.png`, `16-answer-result-x6.png`: Gemini returned a structured step-by-step solution with final answer `x = 6`; the answer screen displayed the original image.
- `17-history-list.png`: Study History showed the image thumbnail and `ANSWERED` status.

The backend started with the dev profile; `dev-status.ps1` reported MySQL, Redis, Mailpit, and backend actuator healthy. The local `backend/.env` uses `APP_BASE_URL=http://10.0.2.2:8080` so signed upload/read URLs are reachable from the AVD. The previous loopback setting (`127.0.0.1`) caused the first upload attempt to fail.

Read-only MySQL verification found an answered question and attachment row with size 11,757 bytes and a 64-character SHA-256. One unattached upload authorization from the earlier failed attempt also remains; TTL cleanup was not awaited. Live access checks returned 404 for a different account, 401 without bearer authentication, 400 when the signed-content token was omitted, and 403 for an invalid signed token. The invalid-token response was initially 500; it now maps to 403, covered by the focused backend integration test.

Generate and push the test images with `scripts/push-test-images.ps1` on Windows or `scripts/push-test-images.sh` on Linux/macOS. The scripts use `adb push` and trigger Android media scanning. The generated images are local test data under `.p09-test-images/` and are intentionally not committed.

The generated set covers:

- Clear equation (`3x - 7 = 11`)
- Handwritten-style math and chemistry formula
- Rotated and low-contrast/blurry equations
- A random PNG larger than 10 MB
- Corrupt PNG bytes
- PDF content with a `.jpg` extension

Remaining emulator checks include camera-permission denial/settings fallback, additional edge-case images, and a camera capture of an equation using a configured webcam or prepared virtual scene. The tested equation was selected from the Photo Picker. Keep any credentials, signed URLs, and bearer tokens out of screenshots and logs.
