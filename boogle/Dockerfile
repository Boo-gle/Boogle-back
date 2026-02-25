# ---------- Build ----------
FROM gradle:9.3-jdk17 AS build
WORKDIR /app

COPY . .
RUN chmod +x ./gradlew

# 앱/배치 둘 다 jar 생성 (테스트 제외)
RUN ./gradlew :app:bootJar :batch:bootJar --no-daemon --stacktrace --console=plain -x test


# ---------- App Runtime ----------
FROM eclipse-temurin:17-jre AS app-runtime
WORKDIR /app

COPY --from=build /app/app/build/libs/ /tmp/app-libs/

# plain.jar는 아예 가정하지 않음 (plain 제외한 jar 1개를 선택)
RUN set -e; \
  APP_JAR="$(find /tmp/app-libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
  if [ -z "$APP_JAR" ]; then \
    echo "[ERROR] app executable jar not found in /tmp/app-libs"; \
    ls -al /tmp/app-libs; \
    exit 1; \
  fi; \
  cp "$APP_JAR" /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]


# ---------- Batch Runtime ----------
FROM eclipse-temurin:17-jre AS batch-runtime
WORKDIR /app

COPY --from=build /app/batch/build/libs/ /tmp/batch-libs/

RUN set -e; \
  BATCH_JAR="$(find /tmp/batch-libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
  if [ -z "$BATCH_JAR" ]; then \
    echo "[ERROR] batch executable jar not found in /tmp/batch-libs"; \
    ls -al /tmp/batch-libs; \
    exit 1; \
  fi; \
  cp "$BATCH_JAR" /app/batch.jar

ENTRYPOINT ["java","-jar","/app/batch.jar"]