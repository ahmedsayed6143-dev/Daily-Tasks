@echo off
setlocal
cd /d "%~dp0"
echo ==============================================
echo      Daily Tasks - Android APK Builder
echo ==============================================
echo.
if not defined ANDROID_HOME (
  if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
)
if not defined ANDROID_HOME (
  echo Android SDK was not found.
  echo Install Android Studio and Android SDK Platform 35 first.
  echo Then run this file again.
  pause
  exit /b 1
)
>local.properties echo sdk.dir=%ANDROID_HOME:\=/%
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo.
  echo Build failed. Check Android Studio and SDK Platform 35 are installed.
  pause
  exit /b 1
)
echo.
echo APK created here:
echo %CD%\app\build\outputs\apk\debug\app-debug.apk
echo.
pause
