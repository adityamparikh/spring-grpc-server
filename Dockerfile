# Multi-stage Dockerfile for Spring gRPC Server
# This creates an optimized, minimal container image for the application

# Stage 1: Build the application using Gradle
FROM gradle:jdk25-alpine AS build
WORKDIR /app

# Copy Gradle configuration files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle

# Copy source code
COPY src src

# Build the application (includes protobuf generation and tests)
RUN gradle build --no-daemon -x test

# Stage 2: Runtime image
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose ports
# 9090: gRPC server
# 8081: Spring Boot Actuator (metrics, health)
EXPOSE 9090 8081

# Health check using grpcurl
# Note: grpcurl must be installed in the image for this to work
# HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
#   CMD grpcurl -plaintext localhost:9090 grpc.health.v1.Health/Check || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
