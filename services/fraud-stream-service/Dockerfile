FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY libs ./libs
COPY services ./services
RUN mvn -q -pl services/fraud-stream-service -am dependency:go-offline

RUN mvn -q -pl services/fraud-stream-service -am -DskipTests package
WORKDIR /workspace/services/fraud-stream-service
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y curl ca-certificates --no-install-recommends && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/services/fraud-stream-service/target/extracted/dependencies/ ./
COPY --from=build /workspace/services/fraud-stream-service/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/services/fraud-stream-service/target/extracted/application/ ./

RUN addgroup --system spring && adduser --system --ingroup spring spring && chown -R spring:spring /app
USER spring

EXPOSE 8083
HEALTHCHECK --interval=15s --timeout=10s --start-period=60s --retries=10 CMD curl -fsS http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
