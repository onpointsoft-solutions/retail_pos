@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo [1/2] Building BizFlow POS with Maven...
if exist "mvnw.cmd" (
    call "mvnw.cmd" clean package
) else (
    where mvn >nul 2>&1
    if errorlevel 1 (
        echo ERROR: Maven was not found. Install Maven or add mvn to PATH.
        exit /b 1
    )
    call mvn clean package
)

if errorlevel 1 (
    echo ERROR: Maven build failed.
    exit /b 1
)

if not exist "target\BizFlowPOS.jar" (
    echo ERROR: target\BizFlowPOS.jar was not generated.
    exit /b 1
)

echo [2/2] Java build complete.
echo Output: %CD%\target\BizFlowPOS.jar
exit /b 0
