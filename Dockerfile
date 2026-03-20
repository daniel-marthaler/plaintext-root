# Multi-stage Dockerfile for plaintext-root
# Stage 1: Build with Maven
# Stage 2: Run with minimal JRE

# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build
COPY pom.xml .
COPY plaintext-root-interfaces/pom.xml plaintext-root-interfaces/
COPY plaintext-root-common/pom.xml plaintext-root-common/
COPY plaintext-root-jpa/pom.xml plaintext-root-jpa/
COPY plaintext-root-menu/pom.xml plaintext-root-menu/
COPY plaintext-root-menu-visibility/pom.xml plaintext-root-menu-visibility/
COPY plaintext-root-role-assignment/pom.xml plaintext-root-role-assignment/
COPY plaintext-root-email/pom.xml plaintext-root-email/
COPY plaintext-root-flyway/pom.xml plaintext-root-flyway/
COPY plaintext-root-discovery/pom.xml plaintext-root-discovery/
COPY plaintext-root-webapp/pom.xml plaintext-root-webapp/
COPY plaintext-root-template-plaintext/pom.xml plaintext-root-template-plaintext/
COPY plaintext-admin-settings/pom.xml plaintext-admin-settings/
COPY plaintext-admin-sessions/pom.xml plaintext-admin-sessions/
COPY plaintext-admin-cron/pom.xml plaintext-admin-cron/
COPY plaintext-admin-value-lists/pom.xml plaintext-admin-value-lists/
COPY plaintext-admin-filelist/pom.xml plaintext-admin-filelist/
COPY plaintext-admin-requirements/pom.xml plaintext-admin-requirements/

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source and build
COPY . .
RUN mvn clean package -DskipTests -B

# ── Runtime stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:25.0.2_10-jre-alpine

# Install bash and wget for healthcheck
RUN apk add --no-cache bash wget

# Create app user with explicit UID/GID to match NAS volume permissions
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# Create application directory
WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /build/plaintext-root-webapp/target/*-exec.jar app.jar

# Copy version files for version display (with graceful fallback)
COPY --chown=1000:1000 version.txt versionRelease.txt ./

# Create directory for logs
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -Duser.timezone=Europe/Zurich"
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Europe/Zurich

# Run the application (using exec form for proper signal handling)
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["java $JAVA_OPTS -jar app.jar"]
