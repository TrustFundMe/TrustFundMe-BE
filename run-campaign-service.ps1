# Script to run Campaign Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

cd "$PSScriptRoot\campaign-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
Write-Host "Starting Campaign Service on port 8082..." -ForegroundColor Green
Write-Host "Note: Ensure MySQL is running and Discovery Server (Eureka) is up" -ForegroundColor Yellow
mvn spring-boot:run
