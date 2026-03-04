# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml .
COPY libs/common/pom.xml libs/common/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml

COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -pl services/notification-service -am -q

COPY libs/common/src libs/common/src
COPY services/notification-service/src services/notification-service/src

RUN ./mvnw package -pl services/notification-service -am -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /workspace/services/notification-service/target/*.jar /app/notification-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/notification-service.jar"]
