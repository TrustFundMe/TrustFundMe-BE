# Script to run Identity Service with H2 Database (no MySQL required)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# Load common functions
$commonFunctionsPath = Join-Path $PSScriptRoot "common-functions.ps1"
if (Test-Path $commonFunctionsPath) {
    . $commonFunctionsPath
}

cd "$PSScriptRoot\..\identity-service"

# Auto-detect and add Maven to PATH
Add-MavenToPath | Out-Null
Write-Host "Starting Identity Service with H2 Database (in-memory)..." -ForegroundColor Green
Write-Host "Note: Data will be lost when service stops (H2 is in-memory database)" -ForegroundColor Yellow
mvn spring-boot:run -Dspring-boot.run.profiles=h2
