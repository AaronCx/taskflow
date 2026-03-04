# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY pom.xml .
COPY libs/common/pom.xml libs/common/pom.xml
COPY services/task-service/pom.xml services/task-service/pom.xml

COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -pl services/task-service -am -q

COPY libs/common/src libs/common/src
COPY services/task-service/src services/task-service/src

RUN ./mvnw package -pl services/task-service -am -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /workspace/services/task-service/target/*.jar /app/task-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/task-service.jar"]
