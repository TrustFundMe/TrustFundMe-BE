# Script to run Identity Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# Load .env file from root directory
$rootDir = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $rootDir ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            # Remove quotes if present
            if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") {
                $value = $matches[1]
            }
            # Map SHARED_* variables to non-prefixed versions for Spring Boot
            if ($key -match '^SHARED_(.+)$') {
                $springKey = $matches[1]
                [Environment]::SetEnvironmentVariable($springKey, $value, "Process")
                Write-Host "  Set $springKey (from $key)" -ForegroundColor Gray
            } else {
                [Environment]::SetEnvironmentVariable($key, $value, "Process")
                Write-Host "  Set $key" -ForegroundColor Gray
            }
        }
    }
} else {
    Write-Host "Warning: .env file not found at $envFile" -ForegroundColor Yellow
}

cd "D:\HOC\Ki 9\TrustFundME- BE\identity-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
Write-Host "Starting Identity Service on port 8081..." -ForegroundColor Green
Write-Host "Note: Ensure MySQL is running and database 'trustfundme_identity_db' exists" -ForegroundColor Yellow
mvn spring-boot:run -e
