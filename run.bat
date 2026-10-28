@echo off
setlocal enabledelayedexpansion

:: Prefer system java or JDK java
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

if not exist "bin\com\hiresphere\Main.class" (
    echo Binaries not found. Running compilation first...
    call build.bat
)

"!JAVA_CMD!" -cp "bin;lib/*;src/main/resources" com.hiresphere.Main
pause
