# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

# Copy parent POM and common lib POM first for dependency caching
COPY pom.xml .
COPY libs/common/pom.xml libs/common/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml

# Copy Maven wrapper
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw

# Resolve dependencies (cached layer)
RUN ./mvnw dependency:go-offline -pl services/api-gateway -am -q

# Copy source
COPY libs/common/src libs/common/src
COPY services/api-gateway/src services/api-gateway/src

# Build the fat JAR (skipping tests — CI handles that separately)
RUN ./mvnw package -pl services/api-gateway -am -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /workspace/services/api-gateway/target/*.jar /app/api-gateway.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/api-gateway.jar"]
