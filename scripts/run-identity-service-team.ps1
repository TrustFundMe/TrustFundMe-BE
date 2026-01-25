# Script to run Identity Service with Team Shared MySQL
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
Write-Host "Starting Identity Service with Team Shared MySQL..." -ForegroundColor Green
Write-Host "Note: Ensure MySQL server is accessible and database 'trustfundme_identity_db' exists" -ForegroundColor Yellow
Write-Host "Update application-team.properties with your MySQL server IP" -ForegroundColor Yellow
mvn spring-boot:run -Dspring-boot.run.profiles=team
