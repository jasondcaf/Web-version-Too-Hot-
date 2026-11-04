
@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo Checking Java installation...
where javac >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java compiler not found.
    echo Install a JDK or configure your PATH.
    pause
    exit /b 1
)

echo Compiling...
if not exist "out" mkdir "out"

set FILES=
for %%f in (*.java) do set FILES=!FILES! "%%f"

if not defined FILES (
    echo ERROR: No Java files found.
    pause
    exit /b 1
)

javac -encoding UTF-8 -d out !FILES!

if errorlevel 1 (
    echo.
    echo COMPILE FAILED - see errors above
    pause
    exit /b 1
)

echo Launching...
start "" javaw -cp out game.Main
