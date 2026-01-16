# Script to run Identity Service with Team Shared MySQL
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

cd "D:\HOC\Ki 9\TrustFundME- BE\identity-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
Write-Host "Starting Identity Service with Team Shared MySQL..." -ForegroundColor Green
Write-Host "Note: Ensure MySQL server is accessible and database 'trustfundme_identity_db' exists" -ForegroundColor Yellow
Write-Host "Update application-team.properties with your MySQL server IP" -ForegroundColor Yellow
mvn spring-boot:run -Dspring-boot.run.profiles=team
