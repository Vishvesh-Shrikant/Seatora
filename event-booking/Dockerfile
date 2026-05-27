# ============================================================
# Phase 1: Build the Application
# ============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ============================================================
# Phase 2: Runtime Environment
# ============================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the compiled jar from Phase 1
COPY --from=builder /app/target/*.jar app.jar

# Expose Tomcat default port
EXPOSE 8080

# Run JVM with optimal garbage collection and memory tuning
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]