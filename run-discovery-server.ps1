# Script to run Discovery Server
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

cd "D:\HOC\Ki 9\TrustFundME- BE\discovery-server"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
Write-Host "Starting Discovery Server on port 8761..." -ForegroundColor Green
mvn spring-boot:run
