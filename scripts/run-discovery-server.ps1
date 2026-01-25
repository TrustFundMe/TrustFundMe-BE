# Script to run Discovery Server
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# Load common functions
$commonFunctionsPath = Join-Path $PSScriptRoot "common-functions.ps1"
if (Test-Path $commonFunctionsPath) {
    . $commonFunctionsPath
}

cd "$PSScriptRoot\..\discovery-server"

# Auto-detect and add Maven to PATH
Add-MavenToPath | Out-Null
Write-Host "Starting Discovery Server on port 8761..." -ForegroundColor Green
mvn spring-boot:run
