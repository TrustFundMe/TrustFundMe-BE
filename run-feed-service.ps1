# Script to run Feed Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

$PSScriptRoot = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition

# Load common functions
$commonFunctionsPath = Join-Path $PSScriptRoot "common-functions.ps1"
if (Test-Path $commonFunctionsPath) {
    . $commonFunctionsPath
}

# Load .env file from root directory (same directory as script)
$rootDir = $PSScriptRoot
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
} else {
    Write-Host "Warning: .env file not found at $envFile" -ForegroundColor Yellow
}

cd "$PSScriptRoot\feed-service"

# Auto-detect and add Maven to PATH
if (Get-Command Add-MavenToPath -ErrorAction SilentlyContinue) {
    Add-MavenToPath | Out-Null
}

Write-Host "Starting Feed Service on port 8084..." -ForegroundColor Green
Write-Host "Environment variables will be inherited by Maven process" -ForegroundColor Gray
mvn spring-boot:run
