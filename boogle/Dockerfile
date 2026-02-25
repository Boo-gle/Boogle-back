# ---------- 1) Build Stage ----------
FROM gradle:9.3-jdk17 AS build
WORKDIR /app

# (중요) 멀티모듈은 부분 COPY로 꼬이기 쉬우니, 아예 전체를 복사해서 경로/구조를 100% 일치시킵니다.
COPY . .

RUN chmod +x ./gradlew

# (중요) compileKotlin만 하지 말고, 실제 산출물(bootJar)까지 만들어야 :app 포함 빌드 그래프가 확실히 잡힙니다.
RUN ./gradlew :app:bootJar :batch:bootJar --no-daemon --stacktrace --info --console=plain -x test


# ---------- 2) App Runtime Stage ----------
FROM eclipse-temurin:17-jre AS app-runtime
WORKDIR /app

COPY --from=build /app/app/build/libs/ /tmp/app-libs/

RUN set -e; \
  APP_JAR_COUNT="$(find /tmp/app-libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | wc -l)"; \
  if [ "$APP_JAR_COUNT" -ne 1 ]; then \
    echo "[ERROR] app jar must be exactly 1 (excluding *-plain.jar). found=$APP_JAR_COUNT"; \
    ls -al /tmp/app-libs; \
    exit 1; \
  fi; \
  APP_JAR_PATH="$(find /tmp/app-libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)"; \
  mv "$APP_JAR_PATH" /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]


# ---------- 3) Batch Runtime Stage ----------
FROM eclipse-temurin:17-jre AS batch-runtime
WORKDIR /app

COPY --from=build /app/batch/build/libs/ /tmp/batch-libs/

RUN set -e; \
  BATCH_JAR_COUNT="$(find /tmp/batch-libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | wc -l)"; \
  if [ "$BATCH_JAR_COUNT" -ne 1 ]; then \
    echo "[ERROR] batch jar must be exactly 1 (excluding *-plain.jar). found=$BATCH_JAR_COUNT"; \
    ls -al /tmp/batch-libs; \
    exit 1; \
  fi; \
  BATCH_JAR_PATH="$(find /tmp/batch-libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)"; \
  mv "$BATCH_JAR_PATH" /app/batch.jar

ENTRYPOINT ["java","-jar","/app/batch.jar"]