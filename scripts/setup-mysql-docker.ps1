# Script to automatically setup MySQL using Docker
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

# Check if MySQL container already exists
$existingContainer = docker ps -a --filter "name=mysql-trustfund" --format "{{.Names}}"
if ($existingContainer -eq "mysql-trustfund") {
    Write-Host "MySQL container already exists!" -ForegroundColor Yellow
    $running = docker ps --filter "name=mysql-trustfund" --format "{{.Names}}"
    if ($running -eq "mysql-trustfund") {
        Write-Host "MySQL container is running!" -ForegroundColor Green
    } else {
        Write-Host "Starting MySQL container..." -ForegroundColor Yellow
        docker start mysql-trustfund
    }
} else {
    Write-Host "Creating new MySQL container..." -ForegroundColor Yellow
    docker run --name mysql-trustfund `
        -e MYSQL_ROOT_PASSWORD=root `
        -e MYSQL_DATABASE=trustfundme_identity_db `
        -p 3306:3306 `
        -d mysql:8.0
    
    Write-Host "Waiting for MySQL to start (30 seconds)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
}

Write-Host "MySQL is ready!" -ForegroundColor Green
Write-Host "Host: localhost" -ForegroundColor Cyan
Write-Host "Port: 3306" -ForegroundColor Cyan
Write-Host "Username: root" -ForegroundColor Cyan
Write-Host "Password: root" -ForegroundColor Cyan
Write-Host "Database: trustfundme_identity_db" -ForegroundColor Cyan
