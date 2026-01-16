# Script to open 3 separate terminals to run all services
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

Write-Host "Opening 3 PowerShell windows to run services..." -ForegroundColor Yellow
Write-Host "Note: Each service will run in a separate window" -ForegroundColor Yellow

# Open Discovery Server
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSScriptRoot\run-discovery-server.ps1`""

# Wait 5 seconds
Start-Sleep -Seconds 5

# Open API Gateway
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSScriptRoot\run-api-gateway.ps1`""

# Wait 5 seconds
Start-Sleep -Seconds 5

# Open Identity Service
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSScriptRoot\run-identity-service.ps1`""

Write-Host "Opened 3 PowerShell windows!" -ForegroundColor Green
Write-Host "1. Discovery Server (port 8761)" -ForegroundColor Cyan
Write-Host "2. API Gateway (port 8080)" -ForegroundColor Cyan
Write-Host "3. Identity Service (port 8081)" -ForegroundColor Cyan
