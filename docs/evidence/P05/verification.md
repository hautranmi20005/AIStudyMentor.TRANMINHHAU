# P05 Verification Evidence

## Backend

Command, from the repository root with `JAVA_HOME` set to `C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot`:

```powershell
Set-Location backend
.\mvnw.cmd -B clean verify
```

Result:

```text
[INFO] Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The test run includes the Testcontainers-backed identity/API tests. An earlier invocation with the default JDK 17 failed because the backend targets Java 25 (`release version 25 not supported`); the successful invocation used the installed JDK 25.0.5.8.

## Android

The recorded P05 Android verification commands completed successfully:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:connectedDebugAndroidTest
```

Results: 2 ViewModel unit tests and 5 connected instrumented tests passed. These checks do not prove that the real backend was reachable.

## Live runtime and screenshots

No P05 screenshot is included. The dev-profile backend did not reach a healthy listening state during the prior runtime attempt, so adult/minor registration against the live stack, Mailpit approval, real token refresh, and profile persistence after force-stop were not verified. Do not treat the automated integration/UI tests as evidence for those live journeys.
