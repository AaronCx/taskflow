# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.15-eclipse-temurin-26-alpine AS builder

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -q

COPY src/ src/
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
