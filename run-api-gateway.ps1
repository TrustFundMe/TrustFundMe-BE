# Script to run API Gateway
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

cd "$PSScriptRoot\api-gateway"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
Write-Host "Starting API Gateway on port 8080..." -ForegroundColor Green
mvn spring-boot:run
