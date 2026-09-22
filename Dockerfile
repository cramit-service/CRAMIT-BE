# syntax=docker/dockerfile:1

FROM eclipse-temurin:17.0.20_8-jdk-jammy AS build
WORKDIR /app

# 의존성만 먼저 받아서 레이어 캐싱 (src 변경 시 매번 다시 받지 않도록)
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17.0.20_8-jre-jammy
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
