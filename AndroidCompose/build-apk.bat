@echo off
rem Double-click to build the APK. Extra options are passed through, e.g.:  build-apk.bat -Install
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-apk.ps1" %*
echo.
pause
