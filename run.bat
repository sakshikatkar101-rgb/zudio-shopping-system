@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   ZUDIO - Online Shopping Management System Runner
echo ========================================================

:: 1. Check for standard Java on PATH or JAVA_HOME
where java >nul 2>nul
if %errorlevel% equ 0 (
    set "JAVA_CMD=java"
) else if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else if exist "C:\Users\sakur\.gemini\antigravity\tools\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Users\sakur\.gemini\antigravity\tools\jdk-17"
    set "JAVA_CMD=C:\Users\sakur\.gemini\antigravity\tools\jdk-17\bin\java.exe"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    echo [ERROR] Java 17+ not found on PATH or JAVA_HOME. Please install JDK 17+.
    pause
    exit /b 1
)

:: 2. Check for standard Maven on PATH
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    set "MVN_CMD=mvn"
) else if exist "C:\Users\sakur\.gemini\antigravity\tools\apache-maven-3.9.6\bin\mvn.cmd" (
    set "MVN_CMD=C:\Users\sakur\.gemini\antigravity\tools\apache-maven-3.9.6\bin\mvn.cmd"
) else (
    echo [ERROR] Maven not found on PATH. Please install Maven.
    pause
    exit /b 1
)

:: 3. Check MySQL service on port 3306
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo [INFO] Starting local MySQL/MariaDB server on port 3306...
    if exist "C:\Users\sakur\.gemini\antigravity\tools\mariadb\bin\mysqld.exe" (
        start /b "" "C:\Users\sakur\.gemini\antigravity\tools\mariadb\bin\mysqld.exe" --datadir="C:\Users\sakur\.gemini\antigravity\tools\mariadb\data" --port=3306 --console
        timeout /t 3 /nobreak >nul
    ) else (
        echo [WARN] MySQL does not seem to be running on port 3306. Ensure MySQL is started.
    )
) else (
    echo [OK] MySQL is active on port 3306.
)

echo [INFO] Compiling and starting ZUDIO application...
call "%MVN_CMD%" compile exec:java

pause
