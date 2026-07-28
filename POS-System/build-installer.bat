@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "APP_NAME=BizFlowPOS"
set "APP_VERSION=2.0.0"
set "INSTALLER_INPUT=%CD%\target\installer-input"
set "APP_IMAGE_ROOT=%CD%\target\package-image"
set "APP_IMAGE=%APP_IMAGE_ROOT%\%APP_NAME%"

echo === BizFlow POS Windows Installer Build ===
call "%~dp0build-java.bat"
if errorlevel 1 exit /b 1

where jpackage >nul 2>&1
if errorlevel 1 (
    echo ERROR: jpackage was not found.
    echo Install a 64-bit JDK 21 and add its bin directory to PATH.
    exit /b 1
)

echo [1/3] Preparing the installer input...
if exist "%INSTALLER_INPUT%" rmdir /s /q "%INSTALLER_INPUT%"
if exist "%APP_IMAGE_ROOT%" rmdir /s /q "%APP_IMAGE_ROOT%"
mkdir "%INSTALLER_INPUT%" || exit /b 1
copy /y "target\BizFlowPOS.jar" "%INSTALLER_INPUT%\BizFlowPOS.jar" >nul || exit /b 1

echo [2/3] Bundling the Java 21 runtime...
jpackage ^
  --type app-image ^
  --input "%INSTALLER_INPUT%" ^
  --dest "%APP_IMAGE_ROOT%" ^
  --name "%APP_NAME%" ^
  --main-jar "BizFlowPOS.jar" ^
  --main-class "com.retailpos.RetailPOS" ^
  --app-version "%APP_VERSION%" ^
  --vendor "BizFlow" ^
  --description "Professional retail point of sale and inventory management" ^
  --icon "%CD%\src\main\resources\BizFlowPOS.ico" ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --add-modules "java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.sql,java.xml,jdk.crypto.ec,jdk.unsupported"

if errorlevel 1 (
    echo ERROR: jpackage failed.
    exit /b 1
)

if not exist "%APP_IMAGE%\BizFlowPOS.exe" (
    echo ERROR: The packaged application executable was not generated.
    exit /b 1
)

> "%APP_IMAGE%\.bizflow-icon-build" echo Icon source: src\main\resources\BizFlowPOS.ico

set "ISCC="
if defined INNO_SETUP_COMPILER if exist "%INNO_SETUP_COMPILER%" set "ISCC=%INNO_SETUP_COMPILER%"
if not defined ISCC if exist "%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" set "ISCC=%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe"
if not defined ISCC if exist "%ProgramFiles%\Inno Setup 6\ISCC.exe" set "ISCC=%ProgramFiles%\Inno Setup 6\ISCC.exe"
if not defined ISCC (
    for /f "delims=" %%I in ('where ISCC.exe 2^>nul') do if not defined ISCC set "ISCC=%%I"
)

if not defined ISCC (
    echo ERROR: Inno Setup 6 was not found.
    echo Install Inno Setup 6, or set INNO_SETUP_COMPILER to the full ISCC.exe path.
    echo The bundled application is available at:
    echo %APP_IMAGE%
    exit /b 1
)

echo [3/3] Compiling the Inno Setup installer...
"%ISCC%" "%CD%\BizFlowPOS.iss"
if errorlevel 1 (
    echo ERROR: Inno Setup compilation failed.
    exit /b 1
)

echo.
echo Installer build complete:
echo %CD%\target\installer\BizFlowPOS-Setup-%APP_VERSION%.exe
exit /b 0
