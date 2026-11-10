@echo off
cd /d "%~dp0"
REM Use Java 25 that ships with the Modrinth App if no system Java 25 is set
if exist "%APPDATA%\ModrinthApp\meta\java_versions\zulu25.34.17-ca-jre25.0.3-win_x64\bin\java.exe" (
  set "JAVA_HOME=%APPDATA%\ModrinthApp\meta\java_versions\zulu25.34.17-ca-jre25.0.3-win_x64"
)
echo Building Vortex Client... (first run downloads Minecraft + a JDK, can take several minutes)
call gradlew.bat installToModrinth --no-daemon --stacktrace > build-log.txt 2>&1
if %ERRORLEVEL%==0 (
  echo BUILD OK >> build-log.txt
  echo Done! Vortex was copied into the Modrinth 26.2 profile.
) else (
  echo BUILD FAILED >> build-log.txt
  echo Build failed - see build-log.txt
)
timeout /t 8
