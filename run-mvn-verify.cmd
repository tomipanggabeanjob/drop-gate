@echo off
"C:\PROGRA~1\Docker\Docker\resources\bin\docker.exe" run --rm -v "d:/drop-gate:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-25 mvn -pl services/order-service -am test > d:\drop-gate\mvn-verify.log 2>&1
echo %ERRORLEVEL% > g:\drop-gate\mvn-verify.exit
