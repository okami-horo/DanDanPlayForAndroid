@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..") do set "PROJECT_ROOT=%%~fI"

set "JAVA_HOME=%PROJECT_ROOT%\.dev-env\jdk\ms-17.0.15"
set "ANDROID_HOME=%PROJECT_ROOT%\.dev-env\android-sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "GRADLE_USER_HOME=%PROJECT_ROOT%\.dev-env\gradle-home"
set "PATH=%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\cmdline-tools\latest\bin;%JAVA_HOME%\bin;%PATH%"

call "%PROJECT_ROOT%\gradlew.bat" %*
exit /b %ERRORLEVEL%
