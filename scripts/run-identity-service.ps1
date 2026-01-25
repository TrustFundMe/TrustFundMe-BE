# Script to run Identity Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# Load common functions
$commonFunctionsPath = Join-Path $PSScriptRoot "common-functions.ps1"
if (Test-Path $commonFunctionsPath) {
    . $commonFunctionsPath
}

# Load .env file from root directory (same directory as script)
$rootDir = Join-Path $PSScriptRoot ".."
$envFile = Join-Path $rootDir ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan
    Write-Host "  .env file path: $envFile" -ForegroundColor Gray
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
    # Verify JWT_SECRET is loaded
    $jwtSecret = [Environment]::GetEnvironmentVariable("JWT_SECRET", "Process")
    if ($jwtSecret) {
        $secretLength = $jwtSecret.Length
        Write-Host "  [OK] JWT_SECRET loaded (length: $secretLength chars)" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] WARNING: JWT_SECRET not found in environment!" -ForegroundColor Red
    }
} else {
    Write-Host "Warning: .env file not found at $envFile" -ForegroundColor Yellow
    Write-Host "  Current script directory: $PSScriptRoot" -ForegroundColor Gray
    Write-Host "  Root directory: $rootDir" -ForegroundColor Gray
}

cd "$PSScriptRoot\..\identity-service"

# Auto-detect and add Maven to PATH
Add-MavenToPath | Out-Null

# Verify JWT_SECRET is set before starting
$jwtSecret = [Environment]::GetEnvironmentVariable("JWT_SECRET", "Process")
if (-not $jwtSecret -or $jwtSecret.Trim() -eq "") {
    Write-Host "[ERROR] JWT_SECRET is not set! Please check your .env file." -ForegroundColor Red
    Write-Host "  Expected .env file at: $envFile" -ForegroundColor Yellow
    exit 1
}

Write-Host "Starting Identity Service on port 8081..." -ForegroundColor Green
Write-Host "Note: Ensure MySQL is running and database 'trustfundme_identity_db' exists" -ForegroundColor Yellow
Write-Host "Environment variables will be inherited by Maven process" -ForegroundColor Gray
mvn spring-boot:run -e
