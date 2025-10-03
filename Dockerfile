# Multi-stage build for Spring Boot application
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Gradle files
COPY gradle gradle
COPY gradlew .
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle/libs.versions.toml gradle/

# Ensure gradlew and wrapper jar are executable
RUN chmod +x gradlew && chmod +x gradle/wrapper/gradle-wrapper.jar

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build application
RUN ./gradlew bootJar --no-daemon

# Production stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S searchlight && adduser -S searchlight -G searchlight

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Create data directory
RUN mkdir -p /app/data && chown -R searchlight:searchlight /app

USER searchlight

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
