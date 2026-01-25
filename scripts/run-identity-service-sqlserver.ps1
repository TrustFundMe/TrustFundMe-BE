# Script to run Identity Service with SQL Server
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
Write-Host "Starting Identity Service with SQL Server..." -ForegroundColor Green
Write-Host "Note: Ensure SQL Server is running and database 'trustfundme_identity_db' exists" -ForegroundColor Yellow
Write-Host "If database doesn't exist, it will be created automatically when service runs" -ForegroundColor Yellow
mvn spring-boot:run -Dspring-boot.run.profiles=sqlserver
