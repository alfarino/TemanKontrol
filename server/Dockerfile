FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew build.gradle.kts ./
COPY gradle ./gradle
COPY shared-dto ./shared-dto
COPY server ./server

# Server-only Gradle config so Ubuntu Docker build skips Android/Windows paths
RUN printf 'kotlin.code.style=official\nkotlin.native.ignoreDisabledTargets=true\n' > gradle.properties && \
    printf 'pluginManagement {\n  repositories { mavenCentral(); gradlePluginPortal(); google() }\n}\ndependencyResolutionManagement {\n  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n  repositories { mavenCentral(); google() }\n}\nrootProject.name = "TemanKontrol"\ninclude(":shared-dto")\ninclude(":server")\n' > settings.gradle.kts && \
    chmod +x gradlew && \
    ./gradlew :server:fatJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache sqlite curl
WORKDIR /app
COPY --from=builder /workspace/server/build/libs/temankontrol-api-all.jar /app/temankontrol-api.jar
RUN mkdir -p /data/db /data/attachments /data/backups
ENV PORT=8080 \
    DB_PATH=/data/db/temankontrol.db \
    MEDIA_DIR=/data/attachments
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -fsS http://localhost:8080/health || exit 1
ENTRYPOINT ["java", "-Xms64m", "-Xmx192m", "-jar", "/app/temankontrol-api.jar"]
