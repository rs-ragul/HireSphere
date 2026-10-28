@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   HireSphere - Launching Web Portal & ATS Backend
echo ===================================================

:: Ensure binaries exist
if not exist "bin\com\hiresphere\Main.class" (
    call build.bat
)

:: Find java
set "JAVA_CMD=java"
if exist "C:\Program Files\Java\jdk-18.0.2\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-18.0.2\bin\java.exe"
) else (
    where java >nul 2>nul
    if %errorlevel% neq 0 (
        if defined JAVA_HOME (
            set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
        )
    )
)

:: Open browser after 2 seconds
start "" cmd /c "timeout /t 2 /nobreak >nul & start http://localhost:8080"

echo Starting server on http://localhost:8080 ...
echo Press Ctrl+C to stop the server.
echo.

"!JAVA_CMD!" -cp "bin;lib/*;src/main/resources" com.hiresphere.Main
pause

