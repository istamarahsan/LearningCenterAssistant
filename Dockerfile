FROM gradle:8-jdk17

WORKDIR /LCA
COPY *.gradle.kts ./
COPY gradle.properties ./
COPY src src

RUN gradle ShadowJar
ENTRYPOINT ["java", "-jar", "build/libs/LearningCenterAssistant.jar"]