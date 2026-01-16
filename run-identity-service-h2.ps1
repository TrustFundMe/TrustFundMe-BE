# Script to run Identity Service with H2 Database (no MySQL required)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

cd "D:\HOC\Ki 9\TrustFundME- BE\identity-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
Write-Host "Starting Identity Service with H2 Database (in-memory)..." -ForegroundColor Green
Write-Host "Note: Data will be lost when service stops (H2 is in-memory database)" -ForegroundColor Yellow
mvn spring-boot:run -Dspring-boot.run.profiles=h2
