# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 레이어 캐시: 빌드 스크립트만 먼저 복사해서 의존성을 받아 둔다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null || true

COPY src src
RUN ./gradlew bootJar -x test --no-daemon \
    && cp build/libs/*-SNAPSHOT.jar app.jar

# ---------- run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home --uid 1001 app \
    && mkdir -p /app/data/csv /app/data/google \
    && chown -R app:app /app
USER app

COPY --from=build --chown=app:app /workspace/app.jar app.jar

# Render는 PORT 환경변수로 포트를 알려준다(application.yml에서 server.port로 사용).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
