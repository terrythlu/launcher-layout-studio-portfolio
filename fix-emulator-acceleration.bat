@echo off
setlocal

net session >nul 2>&1
if errorlevel 1 (
  echo Please run this script as Administrator.
  exit /b 1
)

set "SDK=%USERPROFILE%\AppData\Local\Android\Sdk"
set "DRIVER_DIR=%SDK%\extras\google\Android_Emulator_Hypervisor_Driver"
set "EMUCHECK=%SDK%\emulator\emulator-check.exe"

echo.
echo [1/3] Checking emulator acceleration status...
if exist "%EMUCHECK%" (
  "%EMUCHECK%" accel
) else (
  echo emulator-check.exe not found: %EMUCHECK%
)

echo.
echo [2/3] Installing Android Emulator Hypervisor Driver (AEHD)...
if not exist "%DRIVER_DIR%\silent_install_safe.bat" (
  echo Hypervisor driver installer not found: %DRIVER_DIR%\silent_install_safe.bat
  exit /b 1
)
pushd "%DRIVER_DIR%"
call silent_install_safe.bat
set "INSTALL_EXIT=%ERRORLEVEL%"
popd
if not "%INSTALL_EXIT%"=="0" (
  echo AEHD installation returned exit code %INSTALL_EXIT%.
  exit /b %INSTALL_EXIT%
)

echo.
echo [3/3] Verifying AEHD service and emulator acceleration...
sc query aehd
echo.
if exist "%EMUCHECK%" (
  "%EMUCHECK%" accel
)

echo.
echo Done. If acceleration still does not work, reboot Windows once and retry the emulator.
