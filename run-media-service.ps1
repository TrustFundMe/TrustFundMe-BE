# Script to run Media Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# Load .env file from root directory
$rootDir = $PSScriptRoot
$envFile = Join-Path $rootDir ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") {
                $value = $matches[1]
            }
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "  Set $key" -ForegroundColor Gray
        }
    }
} else {
    Write-Host "Warning: .env file not found at $envFile" -ForegroundColor Yellow
}

cd "$PSScriptRoot\media-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
Write-Host "Starting Media Service on port 8083..." -ForegroundColor Green
Write-Host "Note: Ensure MySQL is running and Discovery Server (Eureka) is up" -ForegroundColor Yellow
mvn spring-boot:run

