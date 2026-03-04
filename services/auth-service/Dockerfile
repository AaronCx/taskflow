# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml .
COPY libs/common/pom.xml libs/common/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml

COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -pl services/auth-service -am -q

COPY libs/common/src libs/common/src
COPY services/auth-service/src services/auth-service/src

RUN ./mvnw package -pl services/auth-service -am -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /workspace/services/auth-service/target/*.jar /app/auth-service.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/auth-service.jar"]
