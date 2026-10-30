@echo off
setlocal
set "ROOT=%~dp0"
set "GRADLE_VERSION=8.9"
set "GRADLE_DIR=%ROOT%tools\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%ROOT%tools\gradle-%GRADLE_VERSION%-bin.zip"
if not exist "%ROOT%tools" mkdir "%ROOT%tools"
if not exist "%GRADLE_DIR%\bin\gradle.bat" (
  echo Downloading Gradle %GRADLE_VERSION%... This may take a few minutes.
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'"
  if errorlevel 1 (echo Failed to download Gradle. Check your internet connection. & exit /b 1)
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%ROOT%tools' -Force"
  if errorlevel 1 (echo Failed to extract Gradle. & exit /b 1)
  del "%GRADLE_ZIP%" >nul 2>&1
)
call "%GRADLE_DIR%\bin\gradle.bat" %*
exit /b %errorlevel%
