# Script to run Identity Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# Load common functions
$commonFunctionsPath = Join-Path $PSScriptRoot "common-functions.ps1"
if (Test-Path $commonFunctionsPath) {
    . $commonFunctionsPath
}

# Load .env (bắt buộc có JWT_SECRET khi chạy MySQL)
Load-EnvFromRoot -RootDir $PSScriptRoot | Out-Null

cd "$PSScriptRoot\identity-service"

# Auto-detect and add Maven to PATH
Add-MavenToPath | Out-Null

# Verify JWT_SECRET is set before starting (MySQL profile không có fallback như H2)
$jwtSecret = [Environment]::GetEnvironmentVariable("JWT_SECRET", "Process")
if (-not $jwtSecret -or $jwtSecret.Trim() -eq "") {
    Write-Host "[ERROR] JWT_SECRET is not set! Add JWT_SECRET to .env at $PSScriptRoot\.env" -ForegroundColor Red
    exit 1
}

Write-Host "Starting Identity Service on port 8081..." -ForegroundColor Green
Write-Host "Note: Ensure MySQL is running and database 'trustfundme_identity_db' exists" -ForegroundColor Yellow
Write-Host "Environment variables will be inherited by Maven process" -ForegroundColor Gray
mvn spring-boot:run -e
