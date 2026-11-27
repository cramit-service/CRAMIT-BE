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

ENV TZ=Asia/Seoul

# STT 교정 단계가 녹음을 배치 구간으로 자른다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
# 전사 중 녹음(최대 200MB)과 m4a faststart 사본을 메모리에 같이 들고 있어서, 기본값(메모리의 1/4)보다 힙을 넓힌다.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
