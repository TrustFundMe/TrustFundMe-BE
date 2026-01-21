# Script to fix Maven JWT dependency issues
# Run this if you get JWT-related Maven errors

Write-Host "=== Fixing Maven JWT Dependencies ===" -ForegroundColor Cyan
Write-Host ""

# Check Java version
Write-Host "1. Checking Java version..." -ForegroundColor Yellow
$javaVersion = java -version 2>&1 | Select-String "version"
Write-Host "   $javaVersion" -ForegroundColor Gray
if ($javaVersion -notmatch "17|18|19|20|21") {
    Write-Host "   [WARNING] Java 17+ is required!" -ForegroundColor Red
}

# Check Maven version
Write-Host ""
Write-Host "2. Checking Maven version..." -ForegroundColor Yellow
$mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven"
Write-Host "   $mavenVersion" -ForegroundColor Gray

Write-Host ""
Write-Host "3. Cleaning Maven projects..." -ForegroundColor Yellow

# Clean all services
$services = @("discovery-server", "api-gateway", "identity-service", "campaign-service", "media-service")

foreach ($service in $services) {
    if (Test-Path $service) {
        Write-Host "   Cleaning $service..." -ForegroundColor Gray
        Set-Location $service
        mvn clean -q
        Set-Location ..
    }
}

Write-Host ""
Write-Host "4. Removing JWT dependencies from local repository..." -ForegroundColor Yellow
$userHome = $env:USERPROFILE
$mavenRepo = Join-Path $userHome ".m2\repository\io\jsonwebtoken"

if (Test-Path $mavenRepo) {
    Write-Host "   Removing: $mavenRepo" -ForegroundColor Gray
    Remove-Item -Path $mavenRepo -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "   [OK] Removed JWT dependencies from local repository" -ForegroundColor Green
} else {
    Write-Host "   [INFO] JWT dependencies not found in local repository" -ForegroundColor Gray
}

Write-Host ""
Write-Host "5. Re-downloading dependencies..." -ForegroundColor Yellow

# Download dependencies for each service
foreach ($service in $services) {
    if (Test-Path $service) {
        Write-Host "   Downloading dependencies for $service..." -ForegroundColor Gray
        Set-Location $service
        mvn dependency:resolve -q
        Set-Location ..
    }
}

Write-Host ""
Write-Host "=== Done! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Try running the service again: .\run-identity-service.ps1" -ForegroundColor White
Write-Host "2. If still having issues, check:" -ForegroundColor White
Write-Host "   - Java version is 17+" -ForegroundColor Gray
Write-Host "   - Maven is properly installed" -ForegroundColor Gray
Write-Host "   - Internet connection (for downloading dependencies)" -ForegroundColor Gray
Write-Host ""
