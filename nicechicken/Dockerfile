# Stage 1: Build Environment (JDK 21 - adjust to your version)
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copy Gradle wrapper and source code
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# Grant execution permission and build (skip tests)
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Stage 2: Runtime Environment
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Set timezone (override with -e TZ=Your/Timezone at runtime)
ENV TZ=UTC

# Fetch the built jar file
COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]