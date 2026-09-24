@echo off
REM ============================================================
REM  RESCUE SCRIPT: Restore HiHonor launcher
REM  Run this if HOME button stops working.
REM  Requires: adb connected to phone via USB.
REM ============================================================
echo Un-suspending HiHonor launcher...
adb shell cmd package unsuspend --user 0 com.hihonor.android.launcher
echo.
echo Re-enabling HiHonor launcher (in case it was disabled)...
adb shell pm enable com.hihonor.android.launcher
echo.
echo Pressing HOME to verify...
adb shell input keyevent KEYCODE_HOME
echo.
echo Done. Press any key to close.
pause >nul
