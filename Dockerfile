ARG TOKEN
FROM eclipse-temurin:17-jdk-alpine AS base

FROM base AS gradle
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
COPY gradle ./gradle
COPY gradlew ./
RUN ./gradlew

FROM gradle AS dependencies
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN ./gradlew :build

FROM dependencies AS build
COPY src ./src
RUN ./gradlew :build

FROM base AS run
COPY --from=build /app/build/distributions/LearningCenterAssistant.tar ./LearningCenterAssistant.tar
RUN tar -xvf LearningCenterAssistant.tar
ENV TOKEN $TOKEN
ENTRYPOINT ["./LearningCenterAssistant/bin/LearningCenterAssistant"]