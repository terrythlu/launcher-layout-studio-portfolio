@echo off
setlocal
set "EMU=%USERPROFILE%\AppData\Local\Android\Sdk\emulator\emulator.exe"
if not exist "%EMU%" (
  echo Emulator not found: %EMU%
  exit /b 1
)

echo Starting Pixel_8_API_35 with hardware acceleration for local verification...
start "" "%EMU%" -avd Pixel_8_API_35 -no-snapshot-load -gpu host
