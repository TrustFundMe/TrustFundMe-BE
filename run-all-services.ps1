# Script to open 4 separate terminals to run all services
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

Write-Host "Opening 5 PowerShell windows to run services..." -ForegroundColor Yellow
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

# Wait 5 seconds
Start-Sleep -Seconds 5

# Open Campaign Service
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSScriptRoot\run-campaign-service.ps1`""

# Wait 5 seconds
Start-Sleep -Seconds 5

# Open Media Service
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSScriptRoot\run-media-service.ps1`""

# Wait 5 seconds
Start-Sleep -Seconds 5

# Open Feed Service
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSScriptRoot\run-feed-service.ps1`""

# Open Moderation Service
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$PSScriptRoot\run-moderation-service.ps1`""

Write-Host "Opened 7 PowerShell windows!" -ForegroundColor Green
Write-Host "1. Discovery Server (port 8761)" -ForegroundColor Cyan
Write-Host "2. API Gateway (port 8080)" -ForegroundColor Cyan
Write-Host "3. Identity Service (port 8081)" -ForegroundColor Cyan
Write-Host "4. Campaign Service (port 8082)" -ForegroundColor Cyan
Write-Host "5. Media Service (port 8083)" -ForegroundColor Cyan
Write-Host "6. Feed Service (port 8084)" -ForegroundColor Cyan
Write-Host "7. Moderation Service (port 8085)" -ForegroundColor Cyan