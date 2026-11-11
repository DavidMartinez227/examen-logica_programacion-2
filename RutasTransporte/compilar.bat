@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -cp "lib/*" -d out src/ruta/*.java
if errorlevel 1 exit /b 1
java -cp "out;lib/*" ruta.Main
