. (Join-Path $PSScriptRoot "dev-common.ps1")
Import-DevEnvironment
Assert-DevEnvironment
Clear-DevProfileOverrides

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java is not installed or is not on PATH. Install the project JDK and retry."
}

Invoke-DevBackendPreflight

Push-Location (Join-Path $script:DevRoot "backend")
try {
    & .\mvnw.cmd -B "-Dmaven.test.skip=true" "-Dspring-boot.run.profiles=dev" spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "Spring Boot exited with code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
    Stop-DevBackend
}
