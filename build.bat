@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   HireSphere ATS - Compiling Java & React Portal
echo ===================================================

if not exist bin mkdir bin

:: Find javac
set "JAVAC_CMD=javac"
where javac >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Program Files\Java\jdk-18.0.2\bin\javac.exe" (
        set "JAVAC_CMD=C:\Program Files\Java\jdk-18.0.2\bin\javac.exe"
    ) else if defined JAVA_HOME (
        set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
    )
)

echo [1/2] Compiling Java Backend (Target: Java 17)...
del /s /q bin\*.class >nul 2>nul
dir /s /b src\main\java\*.java > sources.txt

"!JAVAC_CMD!" --release 17 -d bin -cp "lib/*;src/main/resources" @sources.txt
set COMPILE_STATUS=%errorlevel%
del sources.txt

if %COMPILE_STATUS% neq 0 (
    echo [FAIL] Java compilation failed with error code %COMPILE_STATUS%
    pause
    exit /b %COMPILE_STATUS%
)

if exist src\main\resources\db.properties copy /Y src\main\resources\db.properties bin\ >nul
echo [OK] Java Backend compiled successfully.

:: Build frontend if dist does not exist
if not exist "frontend\dist\index.html" (
    echo.
    echo [2/2] Building React Production Web Portal...
    cd frontend
    call npm.cmd run build
    cd ..
) else (
    echo [2/2] React Web Portal assets ready in 'frontend/dist'.
)

echo.
echo ===================================================
echo   HireSphere Build Complete!
echo   Run 'run-web.bat' to launch the Web Portal.
echo ===================================================
pause
