@echo off
"C:\PROGRA~1\Docker\Docker\resources\bin\docker.exe" run --rm -v "d:/drop-gate:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-25 mvn -B -Dtest=ResilientPaymentGatewayTests -pl services/order-service -am test > d:\drop-gate\mvn-resilience.log 2>&1
echo %ERRORLEVEL% > g:\drop-gate\mvn-resilience.exit
