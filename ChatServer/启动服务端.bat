@echo off
chcp 65001 >nul
title WeiXin ChatServer (port 8888)
cd /d "%~dp0"

echo ============================================
echo   WeiXin ChatServer  -  LAN chat relay
echo ============================================
echo.

where javac >nul 2>nul
if errorlevel 1 (
  echo [ERROR] javac not found in PATH.
  echo         Please install a JDK ^(8 or newer^) and add it to PATH.
  echo         If you already have Android Studio, try:
  echo         C:\Users\lenovo\.jdks\jbr-17.0.14\bin
  pause
  exit /b 1
)

if not exist out mkdir out

echo [1/2] Compiling ChatServer.java ...
javac -encoding UTF-8 -d out src\ChatServer.java
if errorlevel 1 (
  echo [ERROR] Compile failed.
  pause
  exit /b 1
)

echo [2/2] Starting server on port 8888 ...
echo       ^(Keep this window open. Press Ctrl+C to stop.^)
echo.
java -Dfile.encoding=UTF-8 -cp out ChatServer

echo.
echo Server stopped.
pause
