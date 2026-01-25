# Script to automatically setup SQL Server using Docker
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

Write-Host "Checking Docker..." -ForegroundColor Yellow

# Check if Docker is installed
try {
    docker --version | Out-Null
    Write-Host "Docker is installed!" -ForegroundColor Green
} catch {
    Write-Host "Docker is not installed!" -ForegroundColor Red
    Write-Host "Please install Docker Desktop from: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
    exit
}

# Check if SQL Server container already exists
$existingContainer = docker ps -a --filter "name=sqlserver-trustfund" --format "{{.Names}}"
if ($existingContainer -eq "sqlserver-trustfund") {
    Write-Host "SQL Server container already exists!" -ForegroundColor Yellow
    $running = docker ps --filter "name=sqlserver-trustfund" --format "{{.Names}}"
    if ($running -eq "sqlserver-trustfund") {
        Write-Host "SQL Server container is running!" -ForegroundColor Green
    } else {
        Write-Host "Starting SQL Server container..." -ForegroundColor Yellow
        docker start sqlserver-trustfund
    }
} else {
    Write-Host "Creating new SQL Server container..." -ForegroundColor Yellow
    Write-Host "Default password: YourPassword123!" -ForegroundColor Cyan
    docker run --name sqlserver-trustfund `
        -e "ACCEPT_EULA=Y" `
        -e "SA_PASSWORD=YourPassword123!" `
        -p 1433:1433 `
        -d mcr.microsoft.com/mssql/server:2022-latest
    
    Write-Host "Waiting for SQL Server to start (30 seconds)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
}

Write-Host "SQL Server is ready!" -ForegroundColor Green
Write-Host "Host: localhost" -ForegroundColor Cyan
Write-Host "Port: 1433" -ForegroundColor Cyan
Write-Host "Username: sa" -ForegroundColor Cyan
Write-Host "Password: YourPassword123!" -ForegroundColor Cyan
Write-Host "Database: trustfundme_identity_db (will be created automatically)" -ForegroundColor Cyan
