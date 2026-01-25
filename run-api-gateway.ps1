# Script to run API Gateway
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# Load common functions
$commonFunctionsPath = Join-Path $PSScriptRoot "common-functions.ps1"
if (Test-Path $commonFunctionsPath) {
    . $commonFunctionsPath
}

# Load .env (JWT_SECRET phải trùng Identity; nếu không có thì dùng fallback trong application.properties)
Load-EnvFromRoot -RootDir $PSScriptRoot | Out-Null

cd "$PSScriptRoot\api-gateway"

# Auto-detect and add Maven to PATH
Add-MavenToPath | Out-Null
Write-Host "Starting API Gateway on port 8080..." -ForegroundColor Green
mvn spring-boot:run
