# Common functions for all run scripts
# This file contains shared functions that can be used by all service scripts

function Test-TrustFundJavaHomeIsJdk17 {
    param([string]$JavaHome)
    if (-not $JavaHome) { return $false }
    $javaExe = Join-Path $JavaHome "bin\java.exe"
    if (-not (Test-Path $javaExe)) { return $false }
    try {
        $ver = & $javaExe -version 2>&1 | Out-String
        return $ver -match 'version "17\.'
    } catch {
        return $false
    }
}

function Resolve-TrustFundJdk17Home {
    if ($env:TRUSTFUND_JAVA_HOME -and (Test-Path (Join-Path $env:TRUSTFUND_JAVA_HOME "bin\java.exe"))) {
        return $env:TRUSTFUND_JAVA_HOME.TrimEnd('\')
    }
    $roots = @(
        (Join-Path $env:ProgramFiles "Eclipse Adoptium")
        (Join-Path $env:ProgramFiles "Microsoft")
        (Join-Path $env:ProgramFiles "Java")
    )
    $pf86 = ${env:ProgramFiles(x86)}
    if ($pf86) {
        $roots += (Join-Path $pf86 "Eclipse Adoptium")
        $roots += (Join-Path $pf86 "Java")
    }
    $roots = $roots | Where-Object { $_ -and (Test-Path $_) }

    foreach ($root in $roots) {
        $dirs = @()
        try {
            $dirs += Get-ChildItem -Path $root -Directory -ErrorAction Stop |
                Where-Object { $_.Name -match '(?i)^jdk-?17' }
        } catch { continue }
        foreach ($d in ($dirs | Sort-Object Name -Descending)) {
            $jdkHome = $d.FullName
            if (Test-Path (Join-Path $jdkHome "bin\java.exe")) { return $jdkHome }
        }
    }
    return $null
}

function Initialize-TrustFundJdkForMaven {
    <#
    .SYNOPSIS
    Prefer JDK 17 for Maven (matches java.version in pom). Newer JDKs break older Lombok during javac.
    Set TRUSTFUND_JAVA_HOME to override discovery.
    #>
    if (Test-TrustFundJavaHomeIsJdk17 $env:JAVA_HOME) {
        return
    }
    $jdk17 = Resolve-TrustFundJdk17Home
    if (-not $jdk17) {
        Write-Host "[WARNING] JDK 17 not found - set JAVA_HOME or TRUSTFUND_JAVA_HOME to a JDK 17 install." -ForegroundColor Yellow
        Write-Host '  Newer JDK + old Lombok can fail with: TypeTag UNKNOWN (javac / Lombok).' -ForegroundColor Yellow
        return
    }
    $env:JAVA_HOME = $jdk17
    $bin = Join-Path $jdk17 "bin"
    if ($env:Path -notlike "*${bin}*") {
        $env:Path = '{0};{1}' -f $bin, $env:Path
    }
    Write-Host "JAVA_HOME set to JDK 17 for Maven: $jdk17" -ForegroundColor Gray
}

function Add-MavenToPath {
    <#
    .SYNOPSIS
    Automatically detects and adds Maven to PATH if not already present
    #>
    
    Initialize-TrustFundJdkForMaven

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
        "$env:LOCALAPPDATA\Programs\Apache\maven\bin",
        "$env:ProgramFiles\NetBeans-13\netbeans\java\maven\bin"
    )
    
    # Try Chocolatey paths (with wildcard for version)
    $chocoPaths = Get-ChildItem -Path "C:\ProgramData\chocolatey\lib" -Filter "maven*" -Directory -ErrorAction SilentlyContinue
    foreach ($chocoPath in $chocoPaths) {
        $mavenBin = Join-Path $chocoPath.FullName "bin"
        if (Test-Path (Join-Path $mavenBin "mvn.cmd")) {
            $commonMavenPaths += $mavenBin
        }
        # Chocolatey maven package usually extracts to apache-maven-x.x.x
        $subDirs = Get-ChildItem -Path $chocoPath.FullName -Filter "apache-maven*" -Directory -ErrorAction SilentlyContinue
        foreach ($subDir in $subDirs) {
            $subMavenBin = Join-Path $subDir.FullName "bin"
            if (Test-Path (Join-Path $subMavenBin "mvn.cmd")) {
                $commonMavenPaths += $subMavenBin
            }
        }
    }
    
    # Try to find Maven
    foreach ($path in $commonMavenPaths) {
        if (Test-Path $path) {
            $mvnCmd = Join-Path $path "mvn.cmd"
            if (Test-Path $mvnCmd) {
                $env:Path = '{0};{1}' -f $path, $env:Path
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
