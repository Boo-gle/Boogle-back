# FROM docker.elastic.co/elasticsearch/elasticsearch:9.2.3

# 한글 검색을 위한 Nori 플러그인 설치
# RUN #elasticsearch-plugin install analysis-nori

# ---------- 1) Build Stage ----------
FROM gradle:9.3-jdk17 AS build
WORKDIR /app

COPY gradle ./gradle
COPY gradlew gradlew.bat settings.gradle build.gradle ./
RUN chmod +x ./gradlew

COPY app ./app
COPY batch ./batch

RUN ./gradlew :app:bootJar :batch:bootJar -x test


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