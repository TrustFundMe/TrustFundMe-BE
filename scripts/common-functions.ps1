# Common functions for all run scripts
# This file contains shared functions that can be used by all service scripts

function Add-MavenToPath {
    <#
    .SYNOPSIS
    Automatically detects and adds Maven to PATH if not already present
    #>
    
    # Check if Maven is already in PATH
    $mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mavenCmd) {
        Write-Host "Maven found in PATH: $($mavenCmd.Source)" -ForegroundColor Gray
        return $true
    }
    
    # Try common Maven installation paths
    $commonMavenPaths = @(
        "$env:ProgramFiles\Apache\maven\bin",
        "$env:ProgramFiles(x86)\Apache\maven\bin",
        "$env:ProgramFiles\maven\bin",
        "$env:LOCALAPPDATA\Programs\Apache\maven\bin"
    )
    
    # Try Chocolatey paths (with wildcard for version)
    $chocoPaths = Get-ChildItem -Path "C:\ProgramData\chocolatey\lib" -Filter "maven*" -Directory -ErrorAction SilentlyContinue
    foreach ($chocoPath in $chocoPaths) {
        $mavenBin = Join-Path $chocoPath.FullName "bin"
        if (Test-Path (Join-Path $mavenBin "mvn.cmd")) {
            $commonMavenPaths += $mavenBin
        }
    }
    
    # Try to find Maven
    foreach ($path in $commonMavenPaths) {
        if (Test-Path $path) {
            $mvnCmd = Join-Path $path "mvn.cmd"
            if (Test-Path $mvnCmd) {
                $env:Path = "$path;$env:Path"
                Write-Host "Maven found at: $path" -ForegroundColor Gray
                return $true
            }
        }
    }
    
    # Maven not found
    Write-Host "[WARNING] Maven not found in common paths." -ForegroundColor Yellow
    Write-Host "  Please ensure Maven is installed and added to your PATH." -ForegroundColor Yellow
    Write-Host "  Or set MAVEN_HOME environment variable." -ForegroundColor Yellow
    return $false
}
