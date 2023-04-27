FROM eclipse-temurin:17-jdk-alpine AS base

FROM base AS gradle
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
COPY gradle ./gradle
COPY gradlew settings.gradle.kts gradle.properties ./
RUN ./gradlew :wrapper

FROM gradle AS dependencies
COPY build.gradle.kts ./
RUN ./gradlew :build

FROM dependencies AS build
COPY src ./src
RUN ./gradlew :build

FROM base AS run
COPY --from=build /build/distributions/LearningCenterAssistant.tar ./LearningCenterAssistant.tar
RUN tar -xvf LearningCenterAssistant.tar
ENTRYPOINT ["./LearningCenterAssistant/bin/LearningCenterAssistant"]