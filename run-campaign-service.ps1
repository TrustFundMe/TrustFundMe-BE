# Script to run Campaign Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

# Load .env file from root directory (same directory as script)
$rootDir = $PSScriptRoot
$envFile = Join-Path $rootDir ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan
    Write-Host "  .env file path: $envFile" -ForegroundColor Gray
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            # Remove quotes if present
            if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") {
                $value = $matches[1]
            }
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "  Set $key" -ForegroundColor Gray
        }
    }
    # Verify JWT_SECRET is loaded
    $jwtSecret = [Environment]::GetEnvironmentVariable("JWT_SECRET", "Process")
    if ($jwtSecret) {
        $secretLength = $jwtSecret.Length
        Write-Host "  [OK] JWT_SECRET loaded (length: $secretLength chars)" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] WARNING: JWT_SECRET not found in environment!" -ForegroundColor Red
    }
} else {
    Write-Host "Warning: .env file not found at $envFile" -ForegroundColor Yellow
    Write-Host "  Current script directory: $PSScriptRoot" -ForegroundColor Gray
    Write-Host "  Root directory: $rootDir" -ForegroundColor Gray
}

cd "$PSScriptRoot\campaign-service"
$env:Path += ";C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12\bin"
Write-Host "Starting Campaign Service on port 8082..." -ForegroundColor Green
Write-Host "Note: Ensure MySQL is running and Discovery Server (Eureka) is up" -ForegroundColor Yellow
mvn spring-boot:run
