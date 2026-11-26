@echo off
"C:\PROGRA~1\Docker\Docker\resources\bin\docker.exe" run --rm -v "d:/drop-gate:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-25 bash -lc "echo hello-from-container && ls -1 /workspace | head" > d:\drop-gate\docker-hello.log 2>&1
echo %ERRORLEVEL% > g:\drop-gate\docker-hello.exit
