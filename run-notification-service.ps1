# Script to run Notification Service
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$serviceName = "Notification Service"
$serviceDir = "$PSScriptRoot\notification-service"
$port = 8088

# Store original title
$originalTitle = $Host.UI.RawUI.WindowTitle

Try {
    # Set window title
    $Host.UI.RawUI.WindowTitle = "$serviceName (Port $port)"

    Write-Host "===========================" -ForegroundColor Cyan
    Write-Host "Starting $serviceName..." -ForegroundColor Green
    Write-Host "Directory: $serviceDir" -ForegroundColor Yellow
    Write-Host "Port: $port" -ForegroundColor Yellow
    Write-Host "===========================" -ForegroundColor Cyan

    Set-Location $serviceDir
    
    # Run the Spring Boot application using Maven
    mvn spring-boot:run
}
Catch {
    Write-Host "Error running $serviceName`: $_" -ForegroundColor Red
}
Finally {
    # Restore original title
    $Host.UI.RawUI.WindowTitle = $originalTitle
    
    # Keep window open if it crashes
    Write-Host "Press any key to close this window..." -ForegroundColor Yellow
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
