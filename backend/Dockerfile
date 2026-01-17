# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradle gradle
COPY gradlew ./
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]