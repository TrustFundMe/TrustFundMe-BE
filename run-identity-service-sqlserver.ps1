# Script to run Identity Service with SQL Server
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

cd "D:\HOC\Ki 9\TrustFundME- BE\identity-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
Write-Host "Starting Identity Service with SQL Server..." -ForegroundColor Green
Write-Host "Note: Ensure SQL Server is running and database 'trustfundme_identity_db' exists" -ForegroundColor Yellow
Write-Host "If database doesn't exist, it will be created automatically when service runs" -ForegroundColor Yellow
mvn spring-boot:run -Dspring-boot.run.profiles=sqlserver
