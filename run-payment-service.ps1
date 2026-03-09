# Script to run Payment Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# Load common functions
$commonFunctionsPath = Join-Path $PSScriptRoot "common-functions.ps1"
if (Test-Path $commonFunctionsPath) {
    . $commonFunctionsPath
}

# Load environmental variables
if (-not (Get-Command Load-EnvFromRoot -ErrorAction SilentlyContinue)) {
    # Fallback if common-functions.ps1 not loaded or doesn't have the function
    $envFile = Join-Path $PSScriptRoot ".env"
    if (Test-Path $envFile) {
        Get-Content $envFile | ForEach-Object {
            if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
                [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), "Process")
            }
        }
    }
} else {
    Load-EnvFromRoot -RootDir $PSScriptRoot
}

cd "$PSScriptRoot\payment-service"

# Auto-detect and add Maven to PATH
if (Get-Command Add-MavenToPath -ErrorAction SilentlyContinue) {
    Add-MavenToPath | Out-Null
}

Write-Host "Starting Payment Service on port 8087..." -ForegroundColor Green
mvn spring-boot:run
