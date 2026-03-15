# Dockerfile for plaintext-root
# Build JAR locally with Maven, then copy into container

FROM eclipse-temurin:25.0.1_8-jre-alpine

# Install bash and wget for healthcheck
RUN apk add --no-cache bash wget

# Create app user with explicit UID/GID to match NAS volume permissions
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# Create application directory
WORKDIR /app

# Copy pre-built JAR (built locally with Maven)
COPY plaintext-root-webapp/target/*-exec.jar app.jar

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
