# ========================================================
# Stage 1: Build stage with Maven & Eclipse Temurin JDK 17
# ========================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source and static assets
COPY src ./src
COPY frontend ./frontend
COPY sql ./sql

# Package single executable fat JAR
RUN mvn clean package -DskipTests

# ========================================================
# Stage 2: Hardened Runtime with Eclipse Temurin JRE 17
# ========================================================
FROM eclipse-temurin:17-jre-jammy

# Install curl for HEALTHCHECK probe
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Run as non-root user for security
RUN groupadd -g 10001 zudio && \
    useradd -u 10001 -g zudio -s /bin/sh -m zudio

WORKDIR /app

# Copy executable fat JAR from builder stage
COPY --from=builder --chown=zudio:zudio /build/target/zudio.jar /app/zudio.jar

USER zudio

# Set default memory limit for Render 512MB RAM free tier
ENV JAVA_TOOL_OPTIONS="-Xmx350m"

# Expose default HTTP server port
EXPOSE 8080

# Configure container health probe
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/zudio.jar"]
