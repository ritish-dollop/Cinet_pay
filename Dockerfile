# ==============================================================================
# Stage 1: Build Application with Maven Wrapper
# ==============================================================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Install dos2unix to ensure line endings are Unix-compatible
RUN apk add --no-cache dos2unix

# Copy Maven Wrapper & POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Convert line endings and make mvnw executable
RUN dos2unix mvnw && chmod +x mvnw

# Download dependencies (leveraging Docker layer caching)
RUN ./mvnw dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the executable Spring Boot jar
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# Stage 2: Lightweight, Secure Production Runtime
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine AS runner

# Install curl and ca-certificates for health checks & secure outbound calls
RUN apk add --no-cache curl ca-certificates tzdata

# Set timezone
ENV TZ=UTC

# Create unprivileged system user & group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/*-exec.jar /app/app.jar

# Fix permissions
RUN chown -R appuser:appgroup /app

# Run as non-root user
USER appuser

# Expose default application port
EXPOSE 8080

# Production JVM options (optimized for containers)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Built-in container health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
