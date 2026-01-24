# Script to run Moderation Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

$PSScriptRoot = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition

# Load common functions
$commonFunctionsPath = Join-Path $PSScriptRoot "common-functions.ps1"
if (Test-Path $commonFunctionsPath) {
    . $commonFunctionsPath
}

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
        }
    }
}

cd "$PSScriptRoot\flag-service"

# Auto-detect and add Maven to PATH
if (Get-Command Add-MavenToPath -ErrorAction SilentlyContinue) {
    Add-MavenToPath | Out-Null
}

Write-Host "Starting Flag Service on port 8085..." -ForegroundColor Green
mvn spring-boot:run
