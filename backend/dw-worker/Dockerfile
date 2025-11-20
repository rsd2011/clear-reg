# Build with: docker build -t your-registry/dw-worker:TAG .
# Expects bootJar at build/libs/dw-worker-*.jar (run :backend:dw-worker:bootJar first).

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

ARG JAR_FILE=build/libs/dw-worker-*.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java","-jar","/app/app.jar"]
