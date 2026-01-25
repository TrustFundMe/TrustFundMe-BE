# Common functions for all run scripts
# This file contains shared functions that can be used by all service scripts

function Load-EnvFromRoot {
    <#
    .SYNOPSIS
    Load .env from TrustFundMe-BE root. Sets JWT_SECRET, SHARED_* (as SUPABASE_* etc), DB_*, etc.
    .PARAMETER RootDir
    Root directory (default: directory of this script = TrustFundMe-BE root)
    .OUTPUTS
    $true if .env was found and loaded, $false otherwise.
    #>
    param([string]$RootDir = $PSScriptRoot)
    $envFile = Join-Path $RootDir ".env"
    if (-not (Test-Path $envFile)) {
        Write-Host "No .env at $envFile" -ForegroundColor Yellow
        return $false
    }
    Write-Host "Loading .env from $envFile" -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") { $value = $matches[1] }
            if ($key -match '^SHARED_(.+)$') {
                $springKey = $matches[1]
                [Environment]::SetEnvironmentVariable($springKey, $value, "Process")
                Write-Host "  Set $springKey (from $key)" -ForegroundColor Gray
            } else {
                [Environment]::SetEnvironmentVariable($key, $value, "Process")
            }
        }
    }
    $jwt = [Environment]::GetEnvironmentVariable("JWT_SECRET", "Process")
    if ($jwt) { Write-Host "  [OK] JWT_SECRET loaded" -ForegroundColor Green }
    else { Write-Host "  [WARN] JWT_SECRET not in .env (Gateway/Identity use fallback if configured)" -ForegroundColor Yellow }
    return $true
}

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
