# Multi-stage Dockerfile for EduScheduler Pro Backend
# Quick Win #8 - Optimized Docker image with build and runtime stages

# ==========================================================================
# Stage 1: Build Stage
# ==========================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

LABEL stage=builder
LABEL application=eduscheduler-pro

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# ==========================================================================
# Stage 2: Runtime Stage
# ==========================================================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="EduScheduler Team"
LABEL application=eduscheduler-pro
LABEL version=1.0.0

# Install utilities for health checks
RUN apk add --no-cache wget curl

# Create application user (non-root for security)
RUN addgroup -g 1000 eduscheduler && \
    adduser -D -u 1000 -G eduscheduler eduscheduler

# Create application directories
RUN mkdir -p /app/data /app/logs /app/exports && \
    chown -R eduscheduler:eduscheduler /app

WORKDIR /app

# Copy JAR from build stage
COPY --from=build --chown=eduscheduler:eduscheduler \
    /app/target/eduscheduler-pro-1.0.0.jar app.jar

# Switch to non-root user
USER eduscheduler

# Expose ports
EXPOSE 8080 8443

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9590/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Metadata
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=1.0.0

LABEL org.opencontainers.image.created=$BUILD_DATE \
      org.opencontainers.image.title="EduScheduler Pro Backend" \
      org.opencontainers.image.description="AI-Powered School Scheduling System - REST API" \
      org.opencontainers.image.version=$VERSION \
      org.opencontainers.image.revision=$VCS_REF \
      org.opencontainers.image.vendor="EduScheduler" \
      org.opencontainers.image.licenses="Proprietary"
