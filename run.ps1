# ========================================================
# ZUDIO - Online Shopping Management System PowerShell Runner
# ========================================================

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  ZUDIO - Online Shopping Management System Runner" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Resolve Java
$javaCmd = (Get-Command java -ErrorAction SilentlyContinue)
if (-not $javaCmd) {
    if (Test-Path "C:\Users\sakur\.gemini\antigravity\tools\jdk-17\bin\java.exe") {
        $env:JAVA_HOME = "C:\Users\sakur\.gemini\antigravity\tools\jdk-17"
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    } else {
        Write-Error "Java 17+ not found on PATH or JAVA_HOME. Please install JDK 17+."
        exit 1
    }
}

# 2. Resolve Maven
$mvnCmd = "mvn"
$mvnInstalled = (Get-Command mvn -ErrorAction SilentlyContinue)
if (-not $mvnInstalled) {
    if (Test-Path "C:\Users\sakur\.gemini\antigravity\tools\apache-maven-3.9.6\bin\mvn.cmd") {
        $mvnCmd = "C:\Users\sakur\.gemini\antigravity\tools\apache-maven-3.9.6\bin\mvn.cmd"
    } else {
        Write-Error "Maven not found on PATH. Please install Apache Maven."
        exit 1
    }
}

# 3. Check MySQL server
$portActive = (Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue)
if (-not $portActive) {
    Write-Host "[INFO] Starting local MySQL / MariaDB server on port 3306..." -ForegroundColor Yellow
    $mariadbPath = "C:\Users\sakur\.gemini\antigravity\tools\mariadb"
    if (Test-Path "$mariadbPath\bin\mysqld.exe") {
        Start-Process -FilePath "$mariadbPath\bin\mysqld.exe" -ArgumentList "--datadir=`"$mariadbPath\data`" --port=3306 --console" -WindowStyle Hidden
        Start-Sleep -Seconds 3
    } else {
        Write-Warning "MySQL does not appear to be running on port 3306."
    }
} else {
    Write-Host "[OK] MySQL service listening on port 3306." -ForegroundColor Green
}

Write-Host "[INFO] Launching ZUDIO server on http://localhost:8080..." -ForegroundColor Cyan
& $mvnCmd compile exec:java
